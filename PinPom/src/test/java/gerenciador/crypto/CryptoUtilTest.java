package gerenciador.crypto;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {

    @Test
    void cifrarEDecifrarRedondoComMesmaChave() throws Exception {
        SecretKey chave = CryptoUtil.deriveKey("senhaMestre123".toCharArray(), CryptoUtil.randomBytes(16));
        byte[] plano = "dados secretos com acentuacao: ção ~!@#".getBytes(StandardCharsets.UTF_8);

        byte[] cifrado = CryptoUtil.encrypt(plano, chave);
        assertFalse(Arrays.equals(plano, cifrado), "ciphertext nao pode ser igual ao plaintext");

        byte[] decifrado = CryptoUtil.decrypt(cifrado, chave);
        assertArrayEquals(plano, decifrado);
    }

    @Test
    void chaveDiferenteDeveProduzirSaltDiferente() {
        SecretKey k1 = CryptoUtil.deriveKey("senha1".toCharArray(), new byte[16]);
        SecretKey k2 = CryptoUtil.deriveKey("senha2".toCharArray(), new byte[16]);
        assertFalse(Arrays.equals(k1.getEncoded(), k2.getEncoded()));
    }

    @Test
    void senhaIncorretaFalhaNaDescriptografia() throws Exception {
        SecretKey correta = CryptoUtil.deriveKey("correta".toCharArray(), CryptoUtil.randomBytes(16));
        SecretKey errada = CryptoUtil.deriveKey("errada".toCharArray(), CryptoUtil.randomBytes(16));
        byte[] cifrado = CryptoUtil.encrypt("conteudo".getBytes(StandardCharsets.UTF_8), correta);

        assertThrows(CryptoUtil.SenhaIncorretaException.class, () -> CryptoUtil.decrypt(cifrado, errada));
    }

    @Test
    void dadosCorrompidosFalhamNaDescriptografia() throws Exception {
        SecretKey chave = CryptoUtil.deriveKey("senha".toCharArray(), CryptoUtil.randomBytes(16));
        byte[] cifrado = CryptoUtil.encrypt("conteudo".getBytes(StandardCharsets.UTF_8), chave);
        cifrado[cifrado.length - 1] ^= 0x01;

        assertThrows(CryptoUtil.SenhaIncorretaException.class, () -> CryptoUtil.decrypt(cifrado, chave));
    }

    @Test
    void arquivoCurtoDemaisParaSerUmCofre() {
        SecretKey chave = CryptoUtil.deriveKey("senha".toCharArray(), CryptoUtil.randomBytes(16));
        assertThrows(CryptoUtil.SenhaIncorretaException.class, () -> CryptoUtil.decrypt(new byte[4], chave));
    }
}
