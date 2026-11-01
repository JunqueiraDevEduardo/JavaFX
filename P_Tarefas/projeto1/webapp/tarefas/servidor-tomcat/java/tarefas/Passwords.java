// ============================================================================
// Passwords.java  -  guardar e verificar palavras-passe de forma segura
// ============================================================================
// As palavras-passe NUNCA se guardam em texto simples. Guarda-se um "hash"
// calculado com PBKDF2 (PBKDF2WithHmacSHA256), que já vem no Java.
//
// Formato guardado na coluna password_hash:   iterações:sal:hash
//   - iterações: quantas vezes o cálculo é repetido (torna os ataques lentos)
//   - sal:       bytes aleatórios próprios de cada palavra-passe (em Base64)
//   - hash:      o resultado do cálculo (em Base64)
// Exemplo:  120000:79XTbd5n4...:Qw3k...
// ============================================================================
package tarefas;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class Passwords {

    private static final int ITERACOES = 120_000;   // número de repetições do cálculo
    private static final int TAM_SAL = 16;          // tamanho do sal, em bytes
    private static final int TAM_HASH_BITS = 256;   // tamanho do hash, em bits

    // Calcula o hash de uma palavra-passe nova (usado ao criar utilizadores).
    public static String gerarHash(String password) {
        // O sal é aleatório: duas pessoas com a mesma palavra-passe têm hashes diferentes.
        byte[] sal = new byte[TAM_SAL];
        new SecureRandom().nextBytes(sal);

        byte[] hash = pbkdf2(password.toCharArray(), sal, ITERACOES);
        return ITERACOES + ":"
                + Base64.getEncoder().encodeToString(sal) + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    // Verifica se a palavra-passe escrita corresponde ao que está guardado.
    public static boolean verificar(String password, String guardado) {
        // Separa o texto guardado nas três partes.
        String[] p = guardado.split(":");
        int iteracoes = Integer.parseInt(p[0]);
        byte[] sal = Base64.getDecoder().decode(p[1]);
        byte[] esperado = Base64.getDecoder().decode(p[2]);

        // Repete o cálculo com o mesmo sal e as mesmas iterações...
        byte[] calculado = pbkdf2(password.toCharArray(), sal, iteracoes);

        // ...e compara. isEqual demora sempre o mesmo tempo, qualquer que seja o
        // resultado, o que impede descobrir a palavra-passe pelo tempo de resposta.
        return MessageDigest.isEqual(esperado, calculado);
    }

    // O cálculo propriamente dito (PBKDF2 com HMAC-SHA256).
    private static byte[] pbkdf2(char[] password, byte[] sal, int iteracoes) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, sal, iteracoes, TAM_HASH_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
