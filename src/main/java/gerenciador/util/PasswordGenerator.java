package gerenciador.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerador de senhas aleatorias fortes, com opcoes de tamanho e conjunto de
 * caracteres, e um medidor simples de forca baseado em entropia.
 */
public final class PasswordGenerator {

    public static final String MINUSCULAS = "abcdefghijklmnopqrstuvwxyz";
    public static final String MAIUSCULAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String NUMEROS = "0123456789";
    public static final String SIMBOLOS = "!@#$%^&*()-_=+[]{}?<>~";

    // Caracteres ambiguos que costumam confundir na digitacao (l, 1, I, O, 0, ...)
    private static final char[] AMBIGUOS = {'l', '1', 'I', 'O', '0', 'o', '|'};

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {
    }

    /**
     * Gera uma senha aleatoria.
     *
     * @param tamanho      comprimento desejado (6..128)
     * @param maiusculas   inclui letras maiusculas
     * @param minusculas   inclui letras minusculas
     * @param numeros      inclui digitos
     * @param simbolos     inclui simbolos
     * @param semAmbiguos  remove caracteres facilmente confundiveis
     * @return senha gerada
     * @throws IllegalArgumentException se o tamanho for invalido ou nenhum
     *         conjunto de caracteres estiver habilitado
     */
    public static String gerar(int tamanho,
                               boolean maiusculas, boolean minusculas,
                               boolean numeros, boolean simbolos,
                               boolean semAmbiguos) {
        if (tamanho < 6 || tamanho > 128) {
            throw new IllegalArgumentException("Tamanho deve estar entre 6 e 128");
        }

        List<String> conjuntos = new ArrayList<>();
        if (minusculas) conjuntos.add(MINUSCULAS);
        if (maiusculas) conjuntos.add(MAIUSCULAS);
        if (numeros) conjuntos.add(NUMEROS);
        if (simbolos) conjuntos.add(SIMBOLOS);
        if (conjuntos.isEmpty()) {
            throw new IllegalArgumentException("Habilite pelo menos um conjunto de caracteres");
        }

        StringBuilder pool = new StringBuilder();
        for (String c : conjuntos) pool.append(c);
        String poolFinal = semAmbiguos ? removerAmbiguos(pool.toString()) : pool.toString();
        if (poolFinal.isEmpty()) {
            throw new IllegalArgumentException("Nenhum caractere disponivel apos remover os ambiguos");
        }

        // Garante ao menos um caractere de cada conjunto escolhido, depois
        // completa aleatoriamente e embaralha.
        StringBuilder sb = new StringBuilder();
        for (String c : conjuntos) {
            sb.append(pegarCaractere(c, semAmbiguos));
        }
        while (sb.length() < tamanho) {
            sb.append(poolFinal.charAt(RANDOM.nextInt(poolFinal.length())));
        }
        return embaralhar(sb.toString());
    }

    private static char pegarCaractere(String conjunto, boolean semAmbiguos) {
        String c = semAmbiguos ? removerAmbiguos(conjunto) : conjunto;
        return c.charAt(RANDOM.nextInt(c.length()));
    }

    private static String removerAmbiguos(String s) {
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            boolean ambiguo = false;
            for (char a : AMBIGUOS) {
                if (ch == a) {
                    ambiguo = true;
                    break;
                }
            }
            if (!ambiguo) sb.append(ch);
        }
        return sb.toString();
    }

    private static String embaralhar(String senha) {
        char[] chars = senha.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    // ---------------------------------------------------------------
    // Medidor de forca (entropia estimada)
    // ---------------------------------------------------------------

    /**
     * Estima a entropia (em bits) de uma senha com base no conjunto de
     * caracteres que ela parece usar.
     */
    public static double entropia(String senha) {
        if (senha == null || senha.isEmpty()) return 0;
        int pool = 0;
        boolean temMinuscula = false, temMaiuscula = false, temNumero = false, temSimbolo = false;
        for (char c : senha.toCharArray()) {
            if (c >= 'a' && c <= 'z') temMinuscula = true;
            else if (c >= 'A' && c <= 'Z') temMaiuscula = true;
            else if (c >= '0' && c <= '9') temNumero = true;
            else temSimbolo = true;
        }
        if (temMinuscula) pool += 26;
        if (temMaiuscula) pool += 26;
        if (temNumero) pool += 10;
        if (temSimbolo) pool += 33;
        return senha.length() * (Math.log(pool) / Math.log(2));
    }

    /**
     * Classifica a forca em um texto amigavel.
     */
    public static String rotuloForca(double entropia) {
        if (entropia < 50) return "Fraca";
        if (entropia < 80) return "Media";
        if (entropia < 120) return "Forte";
        return "Excelente";
    }
}
