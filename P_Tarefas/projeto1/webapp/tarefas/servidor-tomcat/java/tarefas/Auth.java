// ============================================================================
// Auth.java  -  funções comuns de autenticação e de resposta
// ============================================================================
// Todos os servlets (exceto o de login) começam por verificar se quem faz o
// pedido tem sessão iniciada. Esta classe junta essas verificações e a forma
// de responder em JSON, para não repetir código.
//
// Códigos HTTP usados:
//   401  não estás autenticado (não há sessão)
//   403  estás autenticado, mas não tens permissão (não és administrador)
// ============================================================================
package tarefas;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

public class Auth {

    // Garante que o pedido traz uma sessão com login feito.
    // Devolve a sessão, ou null (depois de responder 401) se não houver.
    public static HttpSession exigirLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // getSession(false) devolve a sessão existente OU null; nunca cria uma nova.
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("id") == null) {
            responderErro(resp, 401, "Sem sessão iniciada");
            return null;
        }
        return s;
    }

    // Garante que quem pede é administrador.
    // Devolve a sessão, ou null (depois de responder 401 ou 403) se não for.
    public static HttpSession exigirAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = exigirLogin(req, resp);
        if (s == null) {
            return null;   // já respondeu 401
        }
        if (!eAdmin(s)) {
            responderErro(resp, 403, "Só os administradores podem fazer isto");
            return null;
        }
        return s;
    }

    // O papel ("ADMIN" ou "UTILIZADOR") foi guardado na sessão no login.
    // Fica no servidor: o cliente não consegue falsificá-lo.
    public static boolean eAdmin(HttpSession s) {
        return "ADMIN".equals(s.getAttribute("papel"));
    }

    // Escreve uma resposta JSON com o código de estado indicado.
    public static void json(HttpServletResponse resp, int status, String corpo) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");   // para os acentos chegarem bem ao cliente
        resp.getWriter().write(corpo);
    }

    // Resposta de erro no formato {"erro":"mensagem"}.
    // O cliente lê a chave "erro" (em minúsculas) para mostrar a mensagem.
    public static void responderErro(HttpServletResponse resp, int status, String mensagem) throws IOException {
        json(resp, status, "{\"erro\":\"" + Json.escapar(mensagem) + "\"}");
    }
}
