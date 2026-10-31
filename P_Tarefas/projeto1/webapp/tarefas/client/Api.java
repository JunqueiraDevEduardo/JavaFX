//Api.java -  o CLIENTE HTTP, faz comunicação e chamadas do método HTTP  para o Servidor TomCat

//Bibliotecas 
import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
	//O cliente HTTP 
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

// Comunica com o servidor Tomcat. O CookieManager guarda o JSESSIONID por nos.
public class Api {
	//URL de comunicação HTTP
    public static final String BASE = "http://localhost:8088/tarefas/api";

//resposta OK , 200 a 300  HTTP, a solicitação ao cliente foi bem sucedida.
    public record Resposta(int status, String texto) {
        public boolean ok() {
            return status >= 200 && status < 300;
        }
		//converte o texto (conteúdo do JSON) num objeto Java. o JSON é dinâmico.
        public Object json() {
            return JsonSimples.ler(texto);
        }
		//o servidor responde a falhas 
        public String erro() {
            try {
				//tentativa de ler o erro JSON
                if (json() instanceof Map<?, ?> m && m.get("erro") != null) {
                    return m.get("erro").toString();
                }
				//Quando o texto do JSON não for válido ou não existir
            } catch (Exception e) {
                
            }
            return "Erro " + status;
        }
    }

//Cliente HTTP, criado uma única vez por Objeto API e guarda e reenvia automaticamente em todos os pedidos seguintes.
    private final HttpClient http = HttpClient.newBuilder()
			//força utilizar o HTTP/1.1
            .version(HttpClient.Version.HTTP_1_1)
			//associa um CookieManager, quando o Tomcat responde ao login com o cabeçalho Set-Cookie:JSESSIONID.
            .cookieHandler(new CookieManager())
            .build();

//Resposta método GET,  consultar 
    public Resposta get(String caminho) throws IOException, InterruptedException {
        //recebe no caminho exemplo /tarefas, constroi o pedido e envia-o para private Resposta enviar(HttpRequest pedido) .
		return enviar(pedido(caminho).GET().build());
    }
//Resposta método POST, adicionar, o texto do JSON é o MAP de dados ex: username=eduardo&password=...
    public Resposta post(String caminho, Map<String, String> dados) throws IOException, InterruptedException {
        //constroi o pedido e envia-o para private Resposta enviar(HttpRequest pedido)
		return enviar(pedido(caminho)
		//mostra o cabeçalho no formato HTML, exatamente o formato que os req.getParameter dos servlets têm
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(codificar(dados)))
                .build());
    }
	    // PUT: atualizar. Substitui o recurso por inteiro, por isso os dados têm de
    // trazer todos os campos (nas tarefas: titulo e concluida).
    public Resposta put(String caminho, Map<String, String> dados) throws IOException, InterruptedException {
        return enviar(pedido(caminho)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .PUT(HttpRequest.BodyPublishers.ofString(codificar(dados)))
                .build());
    }
//Resposta método DELETE, eliminar 
    public Resposta delete(String caminho) throws IOException, InterruptedException {
       //recebe no caminho exemplo /tarefas, elimina o pedido e envia-o  para private Resposta enviar(HttpRequest pedido)
	   return enviar(pedido(caminho).DELETE().build());
    }

//Cria o builder do pedido com o URL completo, HTTPRequest.Builder
    private HttpRequest.Builder pedido(String caminho) {
		//evita a repetição de GET, POST e DELETE e cria  "http://localhost:8088/tarefas/api" + caminho necessário
        return HttpRequest.newBuilder(URI.create(BASE + caminho));
    }
//Realização do envio do pedido dos métodos HTTP
    private Resposta enviar(HttpRequest pedido) throws IOException, InterruptedException {
		//chamar separadamente http.send para cada pedido , garantindo caracteres UTF_8.
        HttpResponse<String> r = http.send(pedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        // guarda na Resposta 
		return new Resposta(r.statusCode(), r.body());
    }
//Codificação  de dados 
    private static String codificar(Map<String, String> dados) {
		//par chave valor separados por & unidos por "="
        StringJoiner sj = new StringJoiner("&");
        for (Map.Entry<String, String> e : dados.entrySet()) {
            sj.add(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
			//passagem pelo URL encoder, garantindo a codificação
                    + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sj.toString();
    }
}
