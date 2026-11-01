// ============================================================================
// LogoutServlet.java  -  POST /api/logout
// ============================================================================
// Termina a sessão: o servidor esquece quem era o utilizador. Depois disto,
// o cookie JSESSIONID que o cliente ainda tem deixa de servir e qualquer
// pedido seguinte recebe 401.
// ============================================================================
package tarefas;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/api/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // false = não criar sessão nova se não existir nenhuma.
        HttpSession s = req.getSession(false);
        if (s != null) {
            s.invalidate();   // apaga a sessão e tudo o que estava guardado nela
        }
        Auth.json(resp, 200, "{\"ok\":true}");
    }
}
