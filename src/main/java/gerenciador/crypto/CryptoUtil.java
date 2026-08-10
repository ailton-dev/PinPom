package gerenciador.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.SecretKey;

/**
 * Funcoes de criptografia usadas pelo cofre de senhas.
 *
 * A chave mestre nunca e armazenada: a cada abertura do app, a senha mestre
 * digitada pelo usuario e usada, junto com um "salt" aleatorio guardado no
 * arquivo do cofre, para derivar uma chave AES-256 via PBKDF2 (100.000
 * iteracoes, HMAC-SHA256). Os dados sao entao cifrados com AES-GCM, que
 * garante tanto sigilo quanto integridade (qualquer adulteracao ou senha
 * incorreta faz a descriptografia falhar).
 */
public final class CryptoUtil {

    public static final int SALT_LEN = 16;      // bytes
    public static final int IV_LEN = 12;        // bytes (recomendado para GCM)
    public static final int GCM_TAG_BITS = 128;  // tamanho da tag de autenticacao
    public static final int PBKDF2_ITERATIONS = 150_000;
    public static final int KEY_LEN_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private CryptoUtil() {
    }

    public static byte[] randomBytes(int length) {
        byte[] b = new byte[length];
        RANDOM.nextBytes(b);
        return b;
    }

    /**
     * Deriva uma chave AES-256 a partir da senha mestre e de um salt.
     */
    public static SecretKey deriveKey(char[] senha, byte[] salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(senha, salt, PBKDF2_ITERATIONS, KEY_LEN_BITS);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Falha ao derivar chave", e);
        }
    }

    /**
     * Cifra os dados com AES-GCM. Retorna: IV (12 bytes) + ciphertext+tag.
     */
    public static byte[] encrypt(byte[] plaintext, SecretKey key) {
        try {
            byte[] iv = randomBytes(IV_LEN);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);
            byte[] out = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao cifrar dados", e);
        }
    }

    /**
     * Decifra dados no formato IV(12) + ciphertext+tag usando AES-GCM.
     * Lanca excecao se a senha estiver incorreta ou os dados corrompidos.
     */
    public static byte[] decrypt(byte[] data, SecretKey key) throws SenhaIncorretaException {
        try {
            if (data.length < IV_LEN) {
                throw new SenhaIncorretaException("Arquivo do cofre invalido");
            }
            byte[] iv = Arrays.copyOfRange(data, 0, IV_LEN);
            byte[] ciphertext = Arrays.copyOfRange(data, IV_LEN, data.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (SenhaIncorretaException e) {
            throw e;
        } catch (Exception e) {
            // Falha de autenticacao do GCM (AEADBadTagException) normalmente
            // significa senha mestre incorreta.
            throw new SenhaIncorretaException("Senha mestre incorreta ou arquivo corrompido");
        }
    }

    /**
     * Excecao lancada quando a senha mestre esta incorreta ou o cofre
     * esta corrompido/adulterado.
     */
    public static class SenhaIncorretaException extends Exception {
        public SenhaIncorretaException(String msg) {
            super(msg);
        }
    }
}
