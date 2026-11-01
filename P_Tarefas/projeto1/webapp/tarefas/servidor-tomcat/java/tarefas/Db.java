// ============================================================================
// Db.java  -  ligação à base de dados MySQL (camada 3)
// ============================================================================
// Cada pedido ao servidor abre uma ligação, executa o SQL e fecha-a
// (com try-with-resources nos servlets). Numa aplicação maior usar-se-ia um
// "pool" de ligações reutilizáveis; para este exemplo basta assim.
//
// ATENÇÃO: se alterares a palavra-passe do utilizador "app_tarefas" no MySQL,
// tens de a alterar aqui também.
// ============================================================================
package tarefas;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {

    // Endereço da base de dados: esta máquina, porta 3306, base "tarefas_db".
    // useUnicode e characterEncoding garantem que os acentos são guardados bem.
    private static final String URL =
            "jdbc:mysql://localhost:3306/tarefas_db?useUnicode=true&characterEncoding=UTF-8";

    // Utilizador criado só para a aplicação, com privilégios mínimos
    // (SELECT, INSERT, UPDATE, DELETE nesta base de dados e mais nada).
    private static final String USER = "app_tarefas";
    private static final String PASS = "App#2026pass";

    // Este bloco corre uma só vez, quando a classe é carregada: regista o driver
    // do MySQL. O ficheiro mysql-connector-j está na pasta lib do Tomcat.
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // Abre uma ligação nova. Quem chamar tem de a fechar (try-with-resources).
    public static Connection ligar() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
