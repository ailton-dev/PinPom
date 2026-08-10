package gerenciador.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordGeneratorTest {

    @Test
    void tamanhoRespeitado() {
        for (int n : new int[]{6, 8, 16, 32, 64}) {
            String senha = PasswordGenerator.gerar(n, true, true, true, true, false);
            assertEquals(n, senha.length(), "tamanho " + n);
        }
    }

    @Test
    void garanteCaractereDeCadaConjuntoEscolhido() {
        for (int i = 0; i < 50; i++) {
            String senha = PasswordGenerator.gerar(16, true, true, true, true, false);
            assertTrue(senha.matches(".*[a-z].*"), "minuscula");
            assertTrue(senha.matches(".*[A-Z].*"), "maiuscula");
            assertTrue(senha.matches(".*[0-9].*"), "numero");
            assertTrue(senha.matches(".*[^a-zA-Z0-9].*"), "simbolo");
        }
    }

    @Test
    void semSimbolosNaoGeraSimbolo() {
        for (int i = 0; i < 50; i++) {
            String senha = PasswordGenerator.gerar(20, true, true, true, false, false);
            assertTrue(senha.matches("[a-zA-Z0-9]+"));
        }
    }

    @Test
    void semAmbiguosExcluiCaracteresConfundiveis() {
        for (int i = 0; i < 50; i++) {
            String senha = PasswordGenerator.gerar(24, true, true, true, true, true);
            for (char c : new char[]{'l', '1', 'I', 'O', '0', 'o', '|'}) {
                assertFalse(senha.indexOf(c) >= 0, "caractere ambiguo presente: " + c);
            }
        }
    }

    @Test
    void conjuntosDiferentesProduzemSenhasDiferentes() {
        String a = PasswordGenerator.gerar(16, true, true, true, true, false);
        String b = PasswordGenerator.gerar(16, true, true, true, true, false);
        assertNotEquals(a, b);
    }

    @Test
    void tamanhoInvalidoLancaExcecao() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordGenerator.gerar(4, true, true, true, true, false));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordGenerator.gerar(200, true, true, true, true, false));
    }

    @Test
    void nenhumConjuntoSelecionadoLancaExcecao() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordGenerator.gerar(16, false, false, false, false, false));
    }

    @Test
    void entropiaDeSenhaVaziaEZero() {
        assertEquals(0, PasswordGenerator.entropia(""));
        assertEquals(0, PasswordGenerator.entropia(null));
    }

    @Test
    void entropiaCresceComTamanho() {
        double curta = PasswordGenerator.entropia("abc");
        double longa = PasswordGenerator.entropia("abcdefghijklmnopqrst");
        assertTrue(longa > curta);
    }
}
