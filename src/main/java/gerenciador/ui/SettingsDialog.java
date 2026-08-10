package gerenciador.ui;

import gerenciador.storage.VaultManager;
import gerenciador.util.ConfigStore;

import javax.swing.*;
import java.awt.*;

/**
 * Configuracoes do aplicativo: bloqueio por inatividade, limpeza da area de
 * transferencia, captura de tela, icones de site e PIN de desbloqueio.
 */
public class SettingsDialog extends JDialog {

    private final JSpinner spinnerBloqueio;
    private final JSpinner spinnerClipboard;
    private final JCheckBox chkCaptura;
    private final JCheckBox chkIcones;
    private final JLabel labelPinStatus;
    private final VaultManager vaultManager;

    public SettingsDialog(Window owner, VaultManager vaultManager) {
        super(owner, "Configuracoes", ModalityType.APPLICATION_MODAL);
        this.vaultManager = vaultManager;

        spinnerBloqueio = new JSpinner(new SpinnerNumberModel(ConfigStore.getBloqueioMinutos(), 0, 240, 1));
        spinnerClipboard = new JSpinner(new SpinnerNumberModel(ConfigStore.getClipboardSegundos(), 0, 300, 1));
        chkCaptura = new JCheckBox("Permitir captura de tela", ConfigStore.isCapturaPermitida());
        chkIcones = new JCheckBox("Mostrar icone de site na tabela", ConfigStore.isIconesSite());
        labelPinStatus = new JLabel(ConfigStore.pinConfigurado()
                ? "PIN configurado" : "PIN nao configurado");

        montarInterface();
        pack();
        setLocationRelativeTo(owner);
    }

    private void montarInterface() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int linha = 0;

        gbc.gridwidth = 2;
        JLabel titulo = new JLabel("Preferencias do aplicativo");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 14f));
        gbc.gridx = 0; gbc.gridy = linha++;
        painel.add(titulo, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = linha;
        painel.add(new JLabel("Bloqueio por inatividade (min):"), gbc);
        gbc.gridx = 1;
        painel.add(spinnerBloqueio, gbc);
        linha++;

        gbc.gridx = 0; gbc.gridy = linha;
        painel.add(new JLabel("Limpar clipboard apos (seg):"), gbc);
        gbc.gridx = 1;
        painel.add(spinnerClipboard, gbc);
        linha++;

        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 2;
        painel.add(chkCaptura, gbc);
        linha++;

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 2;
        painel.add(chkIcones, gbc);
        linha++;
        gbc.gridwidth = 1;

        JLabel dicaCaptura = new JLabel("<html><i>Captura de tela: quando desmarcado, as senhas sao "
                + "ocultadas automaticamente quando a janela perde o foco.</i></html>");
        dicaCaptura.setFont(dicaCaptura.getFont().deriveFont(10f));
        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 2;
        painel.add(dicaCaptura, gbc);
        linha++;
        gbc.gridwidth = 1;

        // ---- PIN ----
        gbc.gridwidth = 2;
        JLabel tituloPin = new JLabel("Desbloqueio rapido (PIN)");
        tituloPin.setFont(tituloPin.getFont().deriveFont(Font.BOLD, 14f));
        gbc.gridx = 0; gbc.gridy = linha++;
        painel.add(tituloPin, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = linha;
        painel.add(labelPinStatus, gbc);

        JPanel painelBotoesPin = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton botaoDefinirPin = new JButton(ConfigStore.pinConfigurado() ? "Alterar PIN" : "Definir PIN");
        botaoDefinirPin.addActionListener(e -> definirPin());
        painelBotoesPin.add(botaoDefinirPin);
        if (ConfigStore.pinConfigurado()) {
            JButton botaoRemoverPin = new JButton("Remover PIN");
            botaoRemoverPin.addActionListener(e -> removerPin());
            painelBotoesPin.add(botaoRemoverPin);
        }
        gbc.gridx = 1; gbc.gridy = linha++;
        painel.add(painelBotoesPin, gbc);

        JLabel dicaPin = new JLabel("<html><i>O PIN desbloqueia o cofre sem digitar a senha mestre. "
                + "Use apenas numeros (4 a 10).</i></html>");
        dicaPin.setFont(dicaPin.getFont().deriveFont(10f));
        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 2;
        painel.add(dicaPin, gbc);
        linha++;
        gbc.gridwidth = 1;

        // ---- Botoes ----
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());
        JButton salvar = new JButton("Salvar");
        salvar.addActionListener(e -> salvar());
        painelBotoes.add(cancelar);
        painelBotoes.add(salvar);

        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 2;
        painel.add(painelBotoes, gbc);

        getRootPane().setDefaultButton(salvar);
        setContentPane(painel);
    }

    private void definirPin() {
        JPasswordField campo = new JPasswordField(10);
        JPasswordField confirmacao = new JPasswordField(10);
        JPanel p = new JPanel(new GridLayout(0, 1, 4, 4));
        p.add(new JLabel("Novo PIN (4 a 10 digitos):"));
        p.add(campo);
        p.add(new JLabel("Confirmar PIN:"));
        p.add(confirmacao);

        int resp = JOptionPane.showConfirmDialog(this, p, "Definir PIN",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resp != JOptionPane.OK_OPTION) return;

        String pin = new String(campo.getPassword());
        String conf = new String(confirmacao.getPassword());
        if (!pin.matches("[0-9]{4,10}")) {
            JOptionPane.showMessageDialog(this, "O PIN deve ter de 4 a 10 digitos numericos.",
                    "PIN invalido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!pin.equals(conf)) {
            JOptionPane.showMessageDialog(this, "Os PINs nao coincidem.",
                    "PIN", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            ConfigStore.definirPin(pin, vaultManager.getChaveAtual().getEncoded());
            labelPinStatus.setText("PIN configurado");
            JOptionPane.showMessageDialog(this, "PIN definido. Use-o na tela de desbloqueio.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao definir PIN: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removerPin() {
        int resp = JOptionPane.showConfirmDialog(this,
                "Remover o PIN de desbloqueio? Voce voltara a usar apenas a senha mestre.",
                "Remover PIN", JOptionPane.YES_NO_OPTION);
        if (resp != JOptionPane.YES_OPTION) return;
        ConfigStore.removerPin();
        labelPinStatus.setText("PIN nao configurado");
        JOptionPane.showMessageDialog(this, "PIN removido.",
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
    }

    private void salvar() {
        ConfigStore.salvarBloqueioMinutos((Integer) spinnerBloqueio.getValue());
        ConfigStore.salvarClipboardSegundos((Integer) spinnerClipboard.getValue());
        ConfigStore.salvarCapturaPermitida(chkCaptura.isSelected());
        ConfigStore.salvarIconesSite(chkIcones.isSelected());
        dispose();
    }
}
