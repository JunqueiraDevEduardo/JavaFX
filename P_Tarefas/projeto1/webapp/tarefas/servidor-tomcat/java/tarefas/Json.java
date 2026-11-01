// ============================================================================
// Json.java  -  utilitário para escrever JSON à mão
// ============================================================================
// O servidor devolve as respostas em JSON, por exemplo {"id":1,"titulo":"Estudar"}.
// Como não usamos nenhuma biblioteca de JSON, o texto é montado com um
// StringBuilder. Se o título de uma tarefa tiver aspas ou mudanças de linha,
// o JSON ficaria partido. Este método "escapa" esses caracteres.
//
// NOTA: este é o ÚNICO nome do método em todo o servidor (escapar). Os
// restantes ficheiros chamam sempre Json.escapar(...).
// ============================================================================
package tarefas;

public class Json {

    // Converte um texto qualquer num texto seguro para meter entre aspas num JSON.
    //   aspas   "   ->  \"
    //   barra   \   ->  \\
    //   mudança de linha, retorno e tabulação  ->  \n  \r  \t
    //   outros caracteres de controlo (código < 0x20)  ->  sequência unicode de 4 dígitos
    public static String escapar(String s) {
        if (s == null) {
            return "";   // evita um erro se vier um valor nulo
        }
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);   // letras, números e acentos passam sem alteração
                    }
                }
            }
        }
        return sb.toString();
    }
}
