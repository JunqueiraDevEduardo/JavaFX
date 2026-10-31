
// Analisador de JSON, nomeado de JsonSimples, em vez de importar bibliotecas, como Gson ou Jackson, optei por 
//escrever um analisador recursivo de JSON.

//Bibliotecas, List para arrays, Map para Objetos
import java.util.ArrayList;
import java.util.LinkedHashMap;//LinkedHasMap em vez do HashMap comum
import java.util.List;
import java.util.Map;


public class JsonSimples {
	
    private final String s;
	//cursor i que irá analisar
    private int i = 0;

//construtor que analisa diretamente sobre a String original s
    private JsonSimples(String s) {
        this.s = s;
    }

//método estático ler, que garante que o analisador JsonSimples seja criado e usado uma só vez
    public static Object ler(String texto) {
        return new JsonSimples(texto).valor();
    }
//Cria e pede um único valor no topo (objeto,array,texto,número,etc...)
    private Object valor() {
		//avança espaços, caracteres, objetos, etc ...
        espacos();
        char c = s.charAt(i);
        if (c == '{') return objeto();
        if (c == '[') return lista();
        if (c == '"') return texto();
        if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
        if (s.startsWith("null", i)) { i += 4; return null; }
		//Se não for nenhum só pode ser um número
        return numero();
    }

//Leitura de Map para objetos
    private Map<String, Object> objeto() {
        Map<String, Object> m = new LinkedHashMap<>();//guarda os pares chave valor
        i++;
        espacos();
        if (s.charAt(i) == '}') { i++; return m; }//percorre {}
		// ciclo guarda os pares chave valor
        while (true) {
            espacos();
            String chave = texto();
            espacos();
            i++;
			//recursão  um valor pode ser outro objeto ou lista, que por sua vez chama valor() de novo
            m.put(chave, valor());
            espacos();
            if (s.charAt(i++) == '}') return m;
        }
    }

//Leitura de List para Arrays
    private List<Object> lista() {
		//guarda o objeto
        List<Object> l = new ArrayList<>();
		//percorre os valores
        i++;
        espacos();
		//começa a percorrer com ]
        if (s.charAt(i) == ']') { i++; return l; }
        while (true) {
            l.add(valor());
            espacos();
		//acaba de percorrer com ]
            if (s.charAt(i++) == ']') return l;
        }
    }
//Leitura de uma String JSON
    private String texto() {
		// guarda a String
        StringBuilder sb = new StringBuilder();
		//lê a string JSON 
        i++;
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') return sb.toString();//acumula caracteres até à aspa de acabar '
            //verifica se existe mudança de linha, tabulação ou código hexadecimal através de '\\'
			if (c == '\\') {
                char e = s.charAt(i++);
                switch (e) {
					//leitura de 4 caracteres
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
						//converter de base 16 para o caracter correspondente
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
					//corresponde a '\\\' ou mais 
                    default -> sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
    }
//Leitura de Números
    private Number numero() {
        int ini = i;
		//o cursor avança enquanto encontrar dígitos, sinais ponto ou expoente
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        String n = s.substring(ini, i);// guarda o valor
		//decisão de que valor de ponto ou expoente  terá
        if (n.contains(".") || n.contains("e") || n.contains("E")) return Double.valueOf(n);
        return Long.valueOf(n);
    }
//leitura de espaços
    private void espacos() {
		//salta espaços em branco, chamado sempre que tiver espaço entre cada elemento , para que o JSON seja lido da mesma maneira
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }
}
