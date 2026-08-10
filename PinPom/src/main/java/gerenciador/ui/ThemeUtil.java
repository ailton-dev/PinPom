package gerenciador.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

/**
 * Aplica o tema claro (FlatLaf Light) ou escuro (FlatLaf Dark) em toda a
 * interface. A preferencia fica salva em ~/.gerenciador-senhas/config.properties.
 */
public final class ThemeUtil {

    private ThemeUtil() {
    }

    public static void aplicarTema(boolean escuro) {
        try {
            if (escuro) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
        } catch (Exception ignored) {
            // Se o FlatLaf falhar, mantem o look and feel corrente.
        }
    }

    /** Re-aplica o tema corrente em todos os componentes de uma janela. */
    public static void atualizarJanela(JFrame janela) {
        SwingUtilities.updateComponentTreeUI(janela);
        janela.pack();
    }
}
