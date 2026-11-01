// ============================================================================
// Forms.java  -  ler o corpo de um formulário nos pedidos PUT
// ============================================================================
// Nos pedidos POST, o Tomcat lê os dados do formulário sozinho e podemos usar
// req.getParameter("titulo"). Nos pedidos PUT NÃO o faz: o corpo fica por ler.
// Esta classe lê o corpo à mão e separa os pares chave=valor.
//
// Exemplo de corpo recebido:   titulo=Estudar+JavaFX&concluida=true
// Resultado:                   {titulo -> "Estudar JavaFX", concluida -> "true"}
// ============================================================================
package tarefas;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Forms {

    // Lê o corpo do pedido e devolve os campos num mapa.
    public static Map<String, String> ler(HttpServletRequest req) throws IOException {
        // Lê todos os bytes do corpo e converte-os em texto UTF-8.
        String corpo = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        Map<String, String> campos = new HashMap<>();
        if (corpo.isBlank()) {
            return campos;   // corpo vazio: nenhum campo
        }

        // O corpo é "chave=valor&chave=valor&...". Separa por "&" e depois por "=".
        for (String par : corpo.split("&")) {
            int i = par.indexOf('=');
            String chave = i < 0 ? par : par.substring(0, i);
            String valor = i < 0 ? "" : par.substring(i + 1);
            // URLDecoder desfaz a codificação: "+" vira espaço, "%C3%A1" vira "á", etc.
            campos.put(URLDecoder.decode(chave, StandardCharsets.UTF_8),
                       URLDecoder.decode(valor, StandardCharsets.UTF_8));
        }
        return campos;
    }
}
