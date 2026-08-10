package gerenciador.util;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;

/**
 * Utilitarios de sistema: abrir links no navegador com fallback para xdg-open
 * (Linux) quando a API Desktop do Java nao funciona no ambiente.
 */
public final class SystemUtil {

    private SystemUtil() {
    }

    /**
     * Abre uma URL (http/https) no navegador padrao.
     */
    public static void abrirUrl(Component owner, String url) {
        try {
            Desktop desktop = Desktop.getDesktop();
            if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new URI(url));
                return;
            }
        } catch (Exception ignored) {
            // tenta o fallback abaixo
        }
        if (!abrirXdgs(owner, "xdg-open", url)) {
            JOptionPane.showMessageDialog(owner,
                    "Nao foi possivel abrir no navegador:\n" + url,
                    "Abrir link", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Abre um e-mail (mailto:) no cliente padrao.
     */
    public static void abrirEmail(Component owner, String email) {
        String destino = "mailto:" + email;
        try {
            Desktop desktop = Desktop.getDesktop();
            if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.MAIL)) {
                desktop.mail(new URI(destino));
                return;
            }
        } catch (Exception ignored) {
            // tenta o fallback abaixo
        }
        if (!abrirXdgs(owner, "xdg-email", email)) {
            JOptionPane.showMessageDialog(owner,
                    "Nao foi possivel abrir o cliente de e-mail:\n" + email,
                    "Abrir e-mail", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static boolean abrirXdgs(Component owner, String comando, String argumento) {
        try {
            Process p = new ProcessBuilder(comando, argumento).start();
            if (p.waitFor() != 0) {
                // falha sem mensagem; o comando pode ter rodado em background
            }
            return true;
        } catch (IOException | InterruptedException ex) {
            return false;
        }
    }
}
