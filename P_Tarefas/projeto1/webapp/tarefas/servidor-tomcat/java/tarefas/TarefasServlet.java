// ============================================================================
// TarefasServlet.java  -  /api/tarefas
// ============================================================================
// Gere as tarefas. Cada método HTTP corresponde a uma operação (CRUD):
//
//   Método   Endereço                 Operação          UTILIZADOR       ADMINISTRADOR
//   ------   ----------------------   ---------------   --------------   ----------------
//   GET      /api/tarefas             ler (listar)      só as suas       todas
//   POST     /api/tarefas             criar             para si          para si
//   PUT      /api/tarefas/{id}        atualizar         só as suas       qualquer uma
//   DELETE   /api/tarefas/{id}        apagar            NÃO (403)        qualquer uma
//
// As regras vivem AQUI, no servidor. O cliente pode esconder botões, mas
// nunca é ele que decide quem pode fazer o quê.
//
// Formato JSON de uma tarefa (as chaves têm de ser iguais no cliente):
//   {"id":1,"titulo":"...","concluida":false,"criadaEm":"...","criador":"eduardo"}
// ============================================================================
package tarefas;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

// O "/*" no fim faz o servlet responder também a /api/tarefas/3, /api/tarefas/7...
@WebServlet("/api/tarefas/*")
public class TarefasServlet extends HttpServlet {

    // ------------------------------------------------------------------------
    // GET /api/tarefas  ->  listar
    // ------------------------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession s = Auth.exigirLogin(req, resp);
        if (s == null) return;   // sem sessão: já respondeu 401

        boolean admin = Auth.eAdmin(s);
        int uid = (Integer) s.getAttribute("id");   // id de quem está autenticado

        // O JOIN junta cada tarefa ao seu criador (nome do utilizador).
        // O utilizador normal só vê as suas: acrescenta-se "WHERE utilizador_id = ?".
        String sql = "SELECT t.id, t.titulo, t.concluida, t.criada_em, u.username "
                + "FROM tarefas t JOIN utilizadores u ON u.id = t.utilizador_id "
                + (admin ? "" : "WHERE t.utilizador_id = ? ")
                + "ORDER BY t.id";

        try (Connection c = Db.ligar(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (!admin) ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {

                // Monta o JSON à mão: [ {...}, {...}, ... ]
                StringBuilder sb = new StringBuilder("[");
                boolean primeiro = true;
                while (rs.next()) {
                    if (!primeiro) sb.append(",");   // vírgula entre elementos
                    primeiro = false;
                    sb.append("{\"id\":").append(rs.getInt("id"))
                      .append(",\"titulo\":\"").append(Json.escapar(rs.getString("titulo")))
                      .append("\",\"concluida\":").append(rs.getBoolean("concluida"))
                      .append(",\"criadaEm\":\"").append(Json.escapar(rs.getString("criada_em")))
                      .append("\",\"criador\":\"").append(Json.escapar(rs.getString("username")))
                      .append("\"}");
                }
                sb.append("]");
                Auth.json(resp, 200, sb.toString());
            }
        } catch (SQLException e) {
            log("Erro na base de dados", e);
            Auth.responderErro(resp, 500, "Erro interno do servidor");
        }
    }

    // ------------------------------------------------------------------------
    // POST /api/tarefas  (campo: titulo)  ->  criar
    // A tarefa fica sempre associada a quem está autenticado.
    // ------------------------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession s = Auth.exigirLogin(req, resp);
        if (s == null) return;
        req.setCharacterEncoding("UTF-8");

        // O POST só serve para criar (/api/tarefas). Qualquer outro caminho é inválido.
        if (partes(req).length != 0) {
            Auth.responderErro(resp, 404, "Não encontrado");
            return;
        }

        String titulo = req.getParameter("titulo");
        if (titulo == null || titulo.isBlank() || titulo.strip().length() > 100) {
            Auth.responderErro(resp, 400, "Título inválido (1 a 100 caracteres)");
            return;
        }

        String sql = "INSERT INTO tarefas (titulo, utilizador_id) VALUES (?, ?)";
        // RETURN_GENERATED_KEYS pede à base de dados o id que acabou de ser criado.
        try (Connection c = Db.ligar();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, titulo.strip());
            ps.setInt(2, (Integer) s.getAttribute("id"));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                // 201 = Created (criado com sucesso)
                Auth.json(resp, 201, "{\"id\":" + keys.getInt(1) + "}");
            }
        } catch (SQLException e) {
            log("Erro na base de dados", e);
            Auth.responderErro(resp, 500, "Erro interno do servidor");
        }
    }

    // ------------------------------------------------------------------------
    // PUT /api/tarefas/{id}  (campos: titulo e concluida)  ->  atualizar
    // O PUT SUBSTITUI a tarefa por inteiro, por isso os dois campos são
    // obrigatórios. Serve para editar o título e para concluir ou reabrir.
    // Utilizador: só as suas tarefas. Administrador: qualquer uma.
    // ------------------------------------------------------------------------
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession s = Auth.exigirLogin(req, resp);
        if (s == null) return;

        // Tem de vir o id no endereço: /api/tarefas/3
        String[] partes = partes(req);
        if (partes.length != 1) {
            Auth.responderErro(resp, 404, "Não encontrado");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(partes[0]);
        } catch (NumberFormatException e) {
            Auth.responderErro(resp, 400, "Id inválido");
            return;
        }

        // No PUT o Tomcat não lê o formulário: usamos a classe Forms.
        Map<String, String> campos = Forms.ler(req);
        String titulo = campos.get("titulo");
        String concluida = campos.get("concluida");

        if (titulo == null || titulo.isBlank() || titulo.strip().length() > 100) {
            Auth.responderErro(resp, 400, "Título inválido (1 a 100 caracteres)");
            return;
        }
        if (!"true".equals(concluida) && !"false".equals(concluida)) {
            Auth.responderErro(resp, 400, "O campo concluida deve ser true ou false");
            return;
        }

        // Regra de permissões: para o utilizador normal acrescenta-se
        // "AND utilizador_id = ?", e assim só consegue alterar tarefas suas.
        boolean admin = Auth.eAdmin(s);
        String sql = "UPDATE tarefas SET titulo = ?, concluida = ? WHERE id = ?"
                + (admin ? "" : " AND utilizador_id = ?");

        try (Connection c = Db.ligar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, titulo.strip());
            ps.setBoolean(2, Boolean.parseBoolean(concluida));
            ps.setInt(3, id);
            if (!admin) ps.setInt(4, (Integer) s.getAttribute("id"));

            // executeUpdate devolve quantas linhas foram alteradas. Se for 0, a
            // tarefa não existe OU não pertence a quem pediu: respondemos 404 nos
            // dois casos, para não revelar que a tarefa de outra pessoa existe.
            if (ps.executeUpdate() == 0) {
                Auth.responderErro(resp, 404, "Tarefa não encontrada");
            } else {
                Auth.json(resp, 200, "{\"ok\":true}");
            }
        } catch (SQLException e) {
            log("Erro na base de dados", e);
            Auth.responderErro(resp, 500, "Erro interno do servidor");
        }
    }

    // ------------------------------------------------------------------------
    // DELETE /api/tarefas/{id}  ->  apagar
    // SÓ O ADMINISTRADOR pode apagar tarefas (e pode apagar qualquer uma).
    // Um utilizador normal recebe 403.
    // ------------------------------------------------------------------------
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // exigirAdmin responde 401 (sem sessão) ou 403 (não é administrador).
        if (Auth.exigirAdmin(req, resp) == null) return;

        String[] partes = partes(req);
        if (partes.length != 1) {
            Auth.responderErro(resp, 404, "Não encontrado");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(partes[0]);
        } catch (NumberFormatException e) {
            Auth.responderErro(resp, 400, "Id inválido");
            return;
        }

        try (Connection c = Db.ligar();
             PreparedStatement ps = c.prepareStatement("DELETE FROM tarefas WHERE id = ?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) {
                Auth.responderErro(resp, 404, "Tarefa não encontrada");
            } else {
                Auth.json(resp, 200, "{\"ok\":true}");
            }
        } catch (SQLException e) {
            log("Erro na base de dados", e);
            Auth.responderErro(resp, 500, "Erro interno do servidor");
        }
    }

    // ------------------------------------------------------------------------
    // Auxiliar: parte do endereço que vem a seguir a /api/tarefas
    //   /api/tarefas      ->  []           (lista vazia)
    //   /api/tarefas/3    ->  ["3"]
    // ------------------------------------------------------------------------
    private static String[] partes(HttpServletRequest req) {
        String p = req.getPathInfo();   // o que vem depois do "/api/tarefas"
        if (p == null || p.equals("/")) return new String[0];
        return p.substring(1).split("/");   // tira a "/" inicial e separa por "/"
    }
}
