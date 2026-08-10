package gerenciador.util;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

/**
 * Carrega a fonte Inter (embutida em resources/fonts) e a define como fonte
 * padrao da interface, dando um visual mais moderno.
 */
public final class FontUtil {

    private static final String[] RECURSOS = {
            "/fonts/Inter-400.ttf",
            "/fonts/Inter-500.ttf",
            "/fonts/Inter-700.ttf"
    };

    private FontUtil() {
    }

    public static void registrarFonte() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (String recurso : RECURSOS) {
                try (InputStream in = FontUtil.class.getResourceAsStream(recurso)) {
                    if (in != null) {
                        ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, in));
                    }
                }
            }
            Font teste = new Font("Inter", Font.PLAIN, 12);
            if ("Inter".equalsIgnoreCase(teste.getFamily())) {
                UIManager.put("defaultFont", new Font("Inter", Font.PLAIN, 13));
                UIManager.put("Table.font", new Font("Inter", Font.PLAIN, 13));
            }
        } catch (Exception ignored) {
            // Se a fonte falhar ao carregar, mantem a fonte padrao do sistema.
        }
    }
}
