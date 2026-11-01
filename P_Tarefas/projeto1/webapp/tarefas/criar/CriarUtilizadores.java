// ============================================================================
// CriarUtilizadores.java  -  ferramenta para criar os utilizadores iniciais
// ============================================================================
// Porquê uma ferramenta à parte? As palavras-passe têm de ser guardadas como
// hash (PBKDF2), e o hash só se consegue gerar em Java. Por isso não se pode
// simplesmente fazer um INSERT no MySQL com a palavra-passe escrita.
//
// O que faz: liga à base de dados e cria dois utilizadores de teste:
//     admin    /  Admin#123   (papel ADMIN)
//     eduardo  /  User#123    (papel UTILIZADOR)
// Se já existirem, repõe a palavra-passe e o papel (ON DUPLICATE KEY UPDATE),
// por isso pode ser executada várias vezes sem dar erro.
//
// Usa a classe tarefas.Passwords do servidor, que já está compilada em
// WEB-INF\classes. Assim o hash é gerado exatamente pelo mesmo código que o
// servidor usa para verificar o login.
//
// COMO USAR (no PowerShell, na pasta onde está este ficheiro):
//
//   $TOMCAT = "C:\Program Files\Apache Software Foundation\Tomcat 11.0"
//   $CLASSES = "<caminho>\servidor-tomcat\WEB-INF\classes"
//   $DRIVER = "$TOMCAT\lib\mysql-connector-j-26.7.0.jar"
//   javac -encoding UTF-8 -cp $CLASSES -d out CriarUtilizadores.java
//   java -cp "out;$CLASSES;$DRIVER" CriarUtilizadores
//
// No Windows o separador do classpath é ";" (em Linux e macOS seria ":").
// ============================================================================

import tarefas.Passwords;   // gerador de hashes do servidor (já compilado)

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CriarUtilizadores {

    // Os mesmos dados de ligação que o servidor usa (ver Db.java).
    static final String URL =
            "jdbc:mysql://localhost:3306/gestao_de_tarefas_base_dados?useUnicode=true&characterEncoding=UTF-8";
    static final String USER = "app_tarefas";
    static final String PASS = "App#2026pass";

    public static void main(String[] args) {
        // try-with-resources: a ligação fecha-se sozinha no fim.
        try (Connection c = DriverManager.getConnection(URL, USER, PASS)) {
            inserir(c, "admin", "admin", "ADMIN");
            inserir(c, "eduardo", "eduardo", "UTILIZADOR");
            System.out.println("Utilizadores criados: admin e eduardo.");
        } catch (SQLException e) {
            // Os erros mais comuns: MySQL desligado, base de dados inexistente,
            // palavra-passe de app_tarefas errada, ou tabela "utilizadores" por criar.
            System.err.println("Não foi possível criar os utilizadores: " + e.getMessage());
            System.exit(1);
        }
    }

    // Insere um utilizador ou, se o nome já existir, atualiza a palavra-passe e o papel.
    static void inserir(Connection c, String username, String password, String papel) throws SQLException {
        // Nunca se guarda a palavra-passe: guarda-se só o hash.
        String hash = Passwords.gerarHash(password);

        // Os "?" são parâmetros (protegem contra SQL injection).
        String sql = "INSERT INTO utilizadores (username, password_hash, papel) VALUES (?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE password_hash = ?, papel = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, papel);
            ps.setString(4, hash);     // valores usados se o utilizador já existir
            ps.setString(5, papel);
            ps.executeUpdate();
        }
    }
}