package gerenciador.ui;

import gerenciador.crypto.CryptoUtil.SenhaIncorretaException;
import gerenciador.model.VaultData;
import gerenciador.storage.VaultManager;
import gerenciador.util.AppInfo;
import gerenciador.util.ConfigStore;
import gerenciador.util.PasswordGenerator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;

/**
 * Tela inicial: se ja existe um cofre, pede a senha mestre (ou o PIN) para
 * abrir. Se nao existe, pede para criar uma nova senha mestre (com confirmacao).
 */
public class LoginFrame extends JFrame {

    private final VaultManager vaultManager;
    private final boolean modoCriacao;

    private JPasswordField campoSenha;
    private JPasswordField campoConfirmacao;
    private JPasswordField campoPin;
    private JLabel labelForca;
    private JLabel labelStatus;
    private JLabel rotuloSenha;
    private JButton botaoAcao;
    private JButton botaoAlternarPin;
    private JPanel hostCampo;
    private boolean usandoPin = false;

    public LoginFrame() {
        super(AppInfo.NOME);
        this.vaultManager = new VaultManager();
        this.modoCriacao = !vaultManager.cofreExiste();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        montarInterface();
        pack();
        setLocationRelativeTo(null);
    }

    private void montarInterface() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titulo = new JLabel("<html><b>PinPom</b> Senhas e Seguranca</html>");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 17f));
        titulo.setHorizontalAlignment(SwingConstants.CENTER);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        painel.add(titulo, gbc);

        JLabel subtitulo = new JLabel(modoCriacao
                ? "Criar senha mestre do cofre"
                : "Digite a senha mestre para abrir o cofre");
        subtitulo.setFont(subtitulo.getFont().deriveFont(Font.PLAIN, 12f));
        subtitulo.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 1;
        painel.add(subtitulo, gbc);

        gbc.gridwidth = 1;
        rotuloSenha = new JLabel(modoCriacao ? "Nova senha mestre:" : "Senha mestre:");
        gbc.gridy = 2; gbc.gridx = 0;
        painel.add(rotuloSenha, gbc);

        campoSenha = new JPasswordField(20);
        campoPin = new JPasswordField(20);
        hostCampo = new JPanel(new CardLayout());
        hostCampo.add(campoSenha, "senha");
        hostCampo.add(campoPin, "pin");
        gbc.gridx = 1;
        painel.add(hostCampo, gbc);

        int proximaLinha = 3;

        if (modoCriacao) {
            campoSenha.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { atualizarForca(); }
                @Override public void removeUpdate(DocumentEvent e) { atualizarForca(); }
                @Override public void changedUpdate(DocumentEvent e) { atualizarForca(); }
            });

            labelForca = new JLabel(" ");
            labelForca.setFont(labelForca.getFont().deriveFont(Font.ITALIC, 11f));
            gbc.gridx = 1; gbc.gridy = proximaLinha++;
            painel.add(labelForca, gbc);

            gbc.gridy = proximaLinha++; gbc.gridx = 0;
            painel.add(new JLabel("Confirmar senha:"), gbc);
            campoConfirmacao = new JPasswordField(20);
            gbc.gridx = 1;
            painel.add(campoConfirmacao, gbc);
        } else if (ConfigStore.pinConfigurado()) {
            botaoAlternarPin = new JButton("Desbloquear com PIN");
            botaoAlternarPin.addActionListener(e -> alternarPin());
            gbc.gridy = proximaLinha++; gbc.gridx = 0; gbc.gridwidth = 2;
            painel.add(botaoAlternarPin, gbc);
        }

        labelStatus = new JLabel(" ");
        labelStatus.setForeground(new Color(0xB00020));
        gbc.gridx = 0; gbc.gridy = proximaLinha++; gbc.gridwidth = 2;
        painel.add(labelStatus, gbc);

        botaoAcao = new JButton(modoCriacao ? "Criar cofre" : "Entrar");
        botaoAcao.addActionListener(e -> {
            if (modoCriacao) criarCofre();
            else if (usandoPin) abrirComPin();
            else abrirCofre();
        });
        gbc.gridy = proximaLinha++;
        painel.add(botaoAcao, gbc);

        getRootPane().setDefaultButton(botaoAcao);

        if (modoCriacao) {
            JLabel dica = new JLabel("<html><i>Guarde bem essa senha: sem ela nao ha como<br>"
                    + "recuperar as senhas salvas (nao ha nenhum tipo de<br>"
                    + "backdoor ou recuperacao).</i></html>");
            dica.setFont(dica.getFont().deriveFont(11f));
            gbc.gridy = proximaLinha++;
            painel.add(dica, gbc);
        }

        setContentPane(painel);
    }

    private void alternarPin() {
        usandoPin = !usandoPin;
        CardLayout layout = (CardLayout) hostCampo.getLayout();
        if (usandoPin) {
            rotuloSenha.setText("PIN:");
            layout.show(hostCampo, "pin");
            botaoAlternarPin.setText("Usar senha mestre");
            botaoAcao.setText("Desbloquear");
            campoPin.requestFocusInWindow();
        } else {
            rotuloSenha.setText("Senha mestre:");
            layout.show(hostCampo, "senha");
            botaoAlternarPin.setText("Desbloquear com PIN");
            botaoAcao.setText("Entrar");
        }
        pack();
    }

    private void atualizarForca() {
        String senha = new String(campoSenha.getPassword());
        if (senha.isEmpty()) {
            labelForca.setText(" ");
            return;
        }
        double entropia = PasswordGenerator.entropia(senha);
        labelForca.setText("Forca: " + PasswordGenerator.rotuloForca(entropia) + " (~" + (int) entropia + " bits)");
        if (entropia < 50) labelForca.setForeground(new Color(0xC62828));
        else if (entropia < 80) labelForca.setForeground(new Color(0xF9A825));
        else if (entropia < 120) labelForca.setForeground(new Color(0x2E7D32));
        else labelForca.setForeground(new Color(0x00695C));
    }

    private void criarCofre() {
        char[] senha = campoSenha.getPassword();
        char[] confirmacao = campoConfirmacao.getPassword();

        if (senha.length < 8) {
            labelStatus.setText("A senha mestre deve ter pelo menos 8 caracteres.");
            return;
        }
        if (isSenhaComum(new String(senha))) {
            labelStatus.setText("Essa senha e muito comum. Escolha uma mais forte.");
            limpar(confirmacao);
            return;
        }
        if (!Arrays.equals(senha, confirmacao)) {
            labelStatus.setText("As senhas nao coincidem.");
            limpar(confirmacao);
            return;
        }
        try {
            vaultManager.criarCofre(senha);
            abrirTelaPrincipal(vaultManager, new VaultData());
        } catch (IOException ex) {
            labelStatus.setText("Erro ao criar cofre: " + ex.getMessage());
        } finally {
            limpar(senha);
            limpar(confirmacao);
        }
    }

    private boolean isSenhaComum(String senha) {
        String s = senha.toLowerCase();
        return "123456".equals(s) || "senha".equals(s) || "password".equals(s)
                || "12345678".equals(s) || "qwerty".equals(s) || "admin".equals(s)
                || "123456789".equals(s) || "abc123".equals(s) || "iloveyou".equals(s)
                || "master".equals(s);
    }

    private void abrirCofre() {
        char[] senha = campoSenha.getPassword();
        try {
            VaultData dados = vaultManager.abrirCofre(senha);
            abrirTelaPrincipal(vaultManager, dados);
        } catch (SenhaIncorretaException ex) {
            labelStatus.setText("Senha incorreta. Tente novamente.");
            campoSenha.setText("");
        } catch (IOException ex) {
            labelStatus.setText("Erro ao ler o cofre: " + ex.getMessage());
        } finally {
            limpar(senha);
        }
    }

    private void abrirComPin() {
        String pin = new String(campoPin.getPassword());
        try {
            VaultData dados = vaultManager.abrirCofreComPin(pin);
            abrirTelaPrincipal(vaultManager, dados);
        } catch (SenhaIncorretaException ex) {
            labelStatus.setText("PIN incorreto. Tente novamente.");
            campoPin.setText("");
        } catch (IOException ex) {
            labelStatus.setText("Erro ao ler o cofre: " + ex.getMessage());
        } finally {
            limpar(pin.toCharArray());
        }
    }

    private void abrirTelaPrincipal(VaultManager vm, VaultData dados) {
        MainFrame principal = new MainFrame(vm, dados);
        principal.setVisible(true);
        dispose();
    }

    private static void limpar(char[] array) {
        Arrays.fill(array, '\0');
    }
}
