package gerenciador.util;

import gerenciador.crypto.CryptoUtil;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;

/**
 * Preferencias do aplicativo, persistidas em ~/.gerenciador-senhas/config.properties.
 *
 * Tambem guarda, de forma segura, a chave do cofre "embrulhada" por um PIN:
 * o PIN nunca e armazenado em claro, apenas a chave AES do cofre cifrada com
 * uma chave derivada do PIN (PBKDF2 + AES-GCM). Isso permite desbloquear o
 * cofre com o PIN sem guardar a senha mestre.
 */
public final class ConfigStore {

    private static final Path CONFIG_DIR =
            Paths.get(System.getProperty("user.home"), ".gerenciador-senhas");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");

    private static final String KEY_TEMA_ESCURO = "tema.escuro";
    private static final String KEY_BLOQUEIO_MINUTOS = "bloqueio.minutos";
    private static final String KEY_CLIPBOARD_SEGUNDOS = "clipboard.segundos";
    private static final String KEY_CAPTURA_PERMITE = "captura.permite";
    private static final String KEY_ICONES_SITE = "icones.site";

    private static final String KEY_PIN_SALT = "pin.salt";
    private static final String KEY_PIN_CHAVE = "pin.chave";
    private static final String KEY_PIN_VERIFICADOR = "pin.verificador";

    private static final byte[] VERIFICADOR = "pinpom-verificador-v1".getBytes(StandardCharsets.UTF_8);

    private ConfigStore() {
    }

    // ---------------------------------------------------------------
    // Tema
    // ---------------------------------------------------------------

    public static void aplicarTemaSalvo() {
        gerenciador.ui.ThemeUtil.aplicarTema(isTemaEscuro());
    }

    public static boolean isTemaEscuro() {
        return Boolean.parseBoolean(carregar().getProperty(KEY_TEMA_ESCURO, "false"));
    }

    public static void salvarTemaEscuro(boolean escuro) {
        setProp(KEY_TEMA_ESCURO, String.valueOf(escuro));
    }

    // ---------------------------------------------------------------
    // Preferencias gerais
    // ---------------------------------------------------------------

    /** Minutos de inatividade para bloquear automaticamente (0 = desativado). */
    public static int getBloqueioMinutos() {
        return parseInt(carregar().getProperty(KEY_BLOQUEIO_MINUTOS, "0"), 0);
    }

    public static void salvarBloqueioMinutos(int minutos) {
        setProp(KEY_BLOQUEIO_MINUTOS, String.valueOf(Math.max(0, minutos)));
    }

    /** Segundos para limpar a area de transferencia apos copiar (0 = desativado). */
    public static int getClipboardSegundos() {
        return parseInt(carregar().getProperty(KEY_CLIPBOARD_SEGUNDOS, "0"), 0);
    }

    public static void salvarClipboardSegundos(int segundos) {
        setProp(KEY_CLIPBOARD_SEGUNDOS, String.valueOf(Math.max(0, segundos)));
    }

    /** Se true, o app permite captura de tela (nada e ocultado ao perder o foco). */
    public static boolean isCapturaPermitida() {
        return Boolean.parseBoolean(carregar().getProperty(KEY_CAPTURA_PERMITE, "false"));
    }

    public static void salvarCapturaPermitida(boolean permitida) {
        setProp(KEY_CAPTURA_PERMITE, String.valueOf(permitida));
    }

    /** Se true, mostra o icone (badge com a letra inicial) na tabela de sites. */
    public static boolean isIconesSite() {
        return Boolean.parseBoolean(carregar().getProperty(KEY_ICONES_SITE, "true"));
    }

    public static void salvarIconesSite(boolean icones) {
        setProp(KEY_ICONES_SITE, String.valueOf(icones));
    }

    // ---------------------------------------------------------------
    // PIN de desbloqueio
    // ---------------------------------------------------------------

    public static boolean pinConfigurado() {
        Properties p = carregar();
        return p.containsKey(KEY_PIN_SALT) && p.containsKey(KEY_PIN_CHAVE) && p.containsKey(KEY_PIN_VERIFICADOR);
    }

    /**
     * Define (ou redefine) o PIN e embrulha a chave do cofre com ele.
     * O PIN deve ter apenas digitos.
     */
    public static void definirPin(String pin, byte[] chaveCofre) {
        if (pin == null || !pin.matches("[0-9]{4,10}")) {
            throw new IllegalArgumentException("O PIN deve ter de 4 a 10 digitos");
        }
        byte[] saltPin = CryptoUtil.randomBytes(CryptoUtil.SALT_LEN);
        SecretKey pinKey = CryptoUtil.deriveKey(pin.toCharArray(), saltPin);
        byte[] encChave = CryptoUtil.encrypt(chaveCofre, pinKey);
        byte[] encVerificador = CryptoUtil.encrypt(VERIFICADOR, pinKey);

        Properties p = carregar();
        p.setProperty(KEY_PIN_SALT, Base64.getEncoder().encodeToString(saltPin));
        p.setProperty(KEY_PIN_CHAVE, Base64.getEncoder().encodeToString(encChave));
        p.setProperty(KEY_PIN_VERIFICADOR, Base64.getEncoder().encodeToString(encVerificador));
        gravar(p);
    }

    public static void removerPin() {
        Properties p = carregar();
        p.remove(KEY_PIN_SALT);
        p.remove(KEY_PIN_CHAVE);
        p.remove(KEY_PIN_VERIFICADOR);
        gravar(p);
    }

    /**
     * Valida o PIN e retorna a chave do cofre embrulhada. Lanca excecao se o
     * PIN estiver incorreto.
     */
    public static byte[] desbloquearComPin(String pin) throws Exception {
        Properties p = carregar();
        byte[] saltPin = Base64.getDecoder().decode(p.getProperty(KEY_PIN_SALT));
        byte[] encChave = Base64.getDecoder().decode(p.getProperty(KEY_PIN_CHAVE));
        byte[] encVerificador = Base64.getDecoder().decode(p.getProperty(KEY_PIN_VERIFICADOR));

        SecretKey pinKey = CryptoUtil.deriveKey(pin.toCharArray(), saltPin);
        byte[] verificado = CryptoUtil.decrypt(encVerificador, pinKey); // falha se o PIN estiver errado
        if (!java.util.Arrays.equals(verificado, VERIFICADOR)) {
            throw new Exception("PIN incorreto");
        }
        return CryptoUtil.decrypt(encChave, pinKey);
    }

    // ---------------------------------------------------------------
    // Interno
    // ---------------------------------------------------------------

    private static void setProp(String chave, String valor) {
        Properties p = carregar();
        p.setProperty(chave, valor);
        gravar(p);
    }

    private static int parseInt(String s, int padrao) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return padrao;
        }
    }

    private static Properties carregar() {
        Properties props = new Properties();
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                props.load(in);
            } catch (IOException ignored) {
                // Preferencia ilegivel: usa valores padrao.
            }
        }
        return props;
    }

    private static void gravar(Properties props) {
        try {
            Files.createDirectories(CONFIG_DIR);
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "Preferencias do PinPom Senhas e Seguranca");
            }
        } catch (IOException ignored) {
            // Falha ao persistir preferencia nao deve impedir o uso do app.
        }
    }
}
