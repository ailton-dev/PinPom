package gerenciador.ui;

import gerenciador.util.AppInfo;
import gerenciador.util.CriadorLinks;
import gerenciador.util.SystemUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Painel "Sobre / Criador": mostra informacoes do projeto e links para o
 * perfil do criador (GitHub, Instagram, e-mail e Reddit).
 */
public class AboutDialog extends JDialog {

    public AboutDialog(Window owner) {
        super(owner, "Sobre o " + AppInfo.NOME, ModalityType.APPLICATION_MODAL);

        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(18, 20, 14, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int linha = 0;

        JLabel titulo = new JLabel(AppInfo.NOME);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = linha++; gbc.gridwidth = 2;
        painel.add(titulo, gbc);

        JLabel versao = new JLabel("Versao " + AppInfo.VERSAO + " - local e 100% offline");
        versao.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = linha++;
        painel.add(versao, gbc);

        JLabel resumo = new JLabel("<html><div style='width:360px;text-align:center'>"
                + "Seu cofre de senhas e identidades cifrado com <b>AES-256-GCM</b>, "
                + "protegido por senha mestre (com opcao de desbloqueio por PIN). "
                + "Os dados ficam apenas no seu computador.</div></html>");
        resumo.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = linha++;
        painel.add(resumo, gbc);

        gbc.gridy = linha++;
        painel.add(Box.createVerticalStrut(6), gbc);

        JLabel criadoPor = new JLabel("Criado por " + CriadorLinks.NOME);
        criadoPor.setFont(criadoPor.getFont().deriveFont(Font.BOLD));
        criadoPor.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = linha++;
        painel.add(criadoPor, gbc);

        JPanel painelLinks = new JPanel(new GridLayout(0, 1, 0, 6));
        painelLinks.setBorder(BorderFactory.createEmptyBorder(6, 20, 6, 20));
        painelLinks.add(botaoLink("GitHub", CriadorLinks.GITHUB));
        painelLinks.add(botaoLink("Instagram", CriadorLinks.INSTAGRAM));
        painelLinks.add(botaoLink("E-mail", CriadorLinks.EMAIL, true));
        painelLinks.add(botaoLink("Reddit", CriadorLinks.REDDIT));
        painelLinks.add(botaoLink("Repositorio do projeto", CriadorLinks.REPOSITORIO));
        gbc.gridy = linha++;
        painel.add(painelLinks, gbc);

        JButton botaoFechar = new JButton("Fechar");
        botaoFechar.addActionListener(e -> dispose());
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));
        painelBotoes.add(botaoFechar);
        gbc.gridy = linha++;
        painel.add(painelBotoes, gbc);

        getRootPane().setDefaultButton(botaoFechar);
        setContentPane(painel);
        pack();
        setLocationRelativeTo(owner);
    }

    private JButton botaoLink(String texto, String url) {
        return botaoLink(texto, url, false);
    }

    private JButton botaoLink(String texto, String url, boolean email) {
        JButton b = new JButton(texto);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(240, 30));
        b.addActionListener(e -> {
            if (email) {
                SystemUtil.abrirEmail(this, url);
            } else {
                SystemUtil.abrirUrl(this, url);
            }
        });
        return b;
    }
}
