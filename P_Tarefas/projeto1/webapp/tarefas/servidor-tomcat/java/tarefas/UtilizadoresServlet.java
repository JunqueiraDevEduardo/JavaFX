// ============================================================================
// UtilizadoresServlet.java  -  /api/utilizadores   (SÓ PARA ADMINISTRADORES)
// ============================================================================
// Gestão de utilizadores. Só existem dois métodos:
//
//   Método   Endereço             Operação         Quem pode
//   ------   ------------------   --------------   ---------------------------
//   GET      /api/utilizadores    listar           só o administrador (senão 403)
//   POST     /api/utilizadores    criar            só o administrador (senão 403)
//
// NÃO há DELETE nem PUT: este servlet não os implementa, por isso o Tomcat
// responde sozinho "405 Method Not Allowed" a esses pedidos.
//
// Formato JSON de um utilizador (as chaves têm de ser iguais no cliente):
//   {"id":1,"username":"admin","papel":"ADMIN"}
// ============================================================================
package tarefas;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

@WebServlet("/api/utilizadores/*")
public class UtilizadoresServlet extends HttpServlet {

    // ------------------------------------------------------------------------
    // GET /api/utilizadores  ->  listar todos os utilizadores
    // Nunca se devolve a palavra-passe nem o hash: só id, nome e papel.
    // ------------------------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (Auth.exigirAdmin(req, resp) == null) return;   // 401 ou 403

        String sql = "SELECT id, username, papel FROM utilizadores ORDER BY id";
        try (Connection c = Db.ligar();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Monta o JSON à mão: [ {...}, {...}, ... ]
            StringBuilder sb = new StringBuilder("[");
            boolean primeiro = true;
            while (rs.next()) {
                if (!primeiro) sb.append(",");
                primeiro = false;
                sb.append("{\"id\":").append(rs.getInt("id"))
                  .append(",\"username\":\"").append(Json.escapar(rs.getString("username")))
                  .append("\",\"papel\":\"").append(rs.getString("papel"))
                  .append("\"}");
            }
            sb.append("]");
            Auth.json(resp, 200, sb.toString());
        } catch (SQLException e) {
            log("Erro na base de dados", e);
            Auth.responderErro(resp, 500, "Erro interno do servidor");
        }
    }

    // ------------------------------------------------------------------------
    // POST /api/utilizadores  (campos: username, password, papel)  ->  criar
    // ------------------------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (Auth.exigirAdmin(req, resp) == null) return;   // 401 ou 403
        req.setCharacterEncoding("UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String papel = req.getParameter("papel");
        if (papel == null || papel.isBlank()) papel = "UTILIZADOR";   // valor por omissão

        // Validações. Todas respondem 400 (pedido inválido).
        if (username == null || username.strip().length() < 3 || username.strip().length() > 50) {
            Auth.responderErro(resp, 400, "O nome de utilizador deve ter entre 3 e 50 caracteres");
            return;
        }
        if (password == null || password.length() < 6) {
            Auth.responderErro(resp, 400, "A palavra-passe deve ter pelo menos 6 caracteres");
            return;
        }
        if (!papel.equals("UTILIZADOR") && !papel.equals("ADMIN")) {
            Auth.responderErro(resp, 400, "Tipo de utilizador inválido");
            return;
        }

        String sql = "INSERT INTO utilizadores (username, password_hash, papel) VALUES (?, ?, ?)";
        try (Connection c = Db.ligar();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username.strip());
            // Guarda-se o HASH, nunca a palavra-passe em texto simples.
            ps.setString(2, Passwords.gerarHash(password));
            ps.setString(3, papel);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                Auth.json(resp, 201, "{\"id\":" + keys.getInt(1) + "}");   // 201 = criado
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // A coluna username é UNIQUE: já existe alguém com esse nome. 409 = conflito.
            Auth.responderErro(resp, 409, "Esse nome de utilizador já existe");
        } catch (SQLException e) {
            log("Erro na base de dados", e);
            Auth.responderErro(resp, 500, "Erro interno do servidor");
        }
    }
}
