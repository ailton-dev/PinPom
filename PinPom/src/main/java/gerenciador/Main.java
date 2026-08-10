package gerenciador;

import gerenciador.ui.LoginFrame;
import gerenciador.util.ConfigStore;
import gerenciador.util.FontUtil;

import javax.swing.*;

/**
 * Gerenciador de Senhas - ponto de entrada da aplicacao.
 *
 * Aplicativo desktop em Java (Swing) para Linux que armazena senhas
 * localmente em um arquivo cifrado com AES-256-GCM, protegido por uma
 * senha mestre definida pelo usuario. Permite importar e exportar
 * entradas em formato CSV.
 */
public class Main {
    public static void main(String[] args) {
        // Carrega a fonte Inter e aplica o tema salvo nas preferencias.
        FontUtil.registrarFonte();
        ConfigStore.aplicarTemaSalvo();

        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
        });
    }
}
