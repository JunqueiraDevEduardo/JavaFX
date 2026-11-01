// ============================================================================
// LoginServlet.java  -  POST /api/login
// ============================================================================
// Recebe "username" e "password", verifica-os na base de dados e, se estiverem
// certos, cria a SESSÃO. O Tomcat envia ao cliente o cookie JSESSIONID, que o
// cliente reenvia em todos os pedidos seguintes.
//
// Respostas:
//   200  {"id":1,"username":"admin","papel":"ADMIN"}
//   400  faltam dados
//   401  utilizador ou palavra-passe errados
//   500  erro na base de dados
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

// A anotação regista o servlet no endereço /api/login (sem ficheiro web.xml).
@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");   // para ler bem acentos nos dados recebidos

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || password == null || username.isBlank() || password.isEmpty()) {
            Auth.responderErro(resp, 400, "Falta o nome de utilizador ou a palavra-passe");
            return;
        }

        // O "?" é um parâmetro: o valor é enviado à parte do SQL, o que impede
        // ataques de "SQL injection".
        String sql = "SELECT id, username, password_hash, papel FROM utilizadores WHERE username = ?";
        try (Connection c = Db.ligar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {

                // Se o utilizador existe E a palavra-passe corresponde ao hash guardado:
                if (rs.next() && Passwords.verificar(password, rs.getString("password_hash"))) {
                    int id = rs.getInt("id");
                    String nome = rs.getString("username");
                    String papel = rs.getString("papel");

                    // Termina qualquer sessão anterior antes de criar uma nova.
                    // Isto evita "session fixation" (reaproveitar um identificador
                    // de sessão previamente conhecido por um atacante).
                    HttpSession antiga = req.getSession(false);
                    if (antiga != null) {
                        antiga.invalidate();
                    }

                    // Cria a sessão nova e guarda nela quem é o utilizador.
                    // Estes dados ficam NO SERVIDOR; o cliente só recebe o cookie.
                    HttpSession sessao = req.getSession(true);
                    sessao.setAttribute("id", id);
                    sessao.setAttribute("username", nome);
                    sessao.setAttribute("papel", papel);

                    Auth.json(resp, 200, "{\"id\":" + id
                            + ",\"username\":\"" + Json.escapar(nome)
                            + "\",\"papel\":\"" + papel + "\"}");
                    return;
                }
            }
        } catch (SQLException e) {
            log("Erro na base de dados", e);   // fica no registo do Tomcat (logs)
            Auth.responderErro(resp, 500, "Erro interno do servidor");
            return;
        }

        // Mesma mensagem quando o utilizador não existe e quando a palavra-passe
        // está errada: assim não se revela quais os nomes de utilizador que existem.
        Auth.responderErro(resp, 401, "Utilizador ou palavra-passe errados");
    }
}
