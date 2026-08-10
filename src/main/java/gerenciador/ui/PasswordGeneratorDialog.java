package gerenciador.ui;

import gerenciador.util.PasswordGenerator;

import javax.swing.*;
import java.awt.*;

/**
 * Dialogo para gerar uma senha aleatoria com opcoes de tamanho e conjunto de
 * caracteres. Exibe uma pre-visualizacao em tempo real.
 */
public class PasswordGeneratorDialog extends JDialog {

    private final JSpinner campoTamanho;
    private final JCheckBox chkMaiusculas;
    private final JCheckBox chkMinusculas;
    private final JCheckBox chkNumeros;
    private final JCheckBox chkSimbolos;
    private final JCheckBox chkSemAmbiguos;
    private final JTextField previsualizacao;

    private boolean confirmado = false;
    private String senhaGerada = "";

    public PasswordGeneratorDialog(Window owner) {
        super(owner, "Gerador de senha forte", ModalityType.APPLICATION_MODAL);

        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int linha = 0;

        JLabel rotuloTamanho = new JLabel("Tamanho:");
        campoTamanho = new JSpinner(new SpinnerNumberModel(16, 6, 128, 1));
        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 1;
        painel.add(rotuloTamanho, gbc);
        gbc.gridx = 1;
        painel.add(campoTamanho, gbc);
        linha++;

        JPanel painelOpcoes = new JPanel(new GridLayout(0, 1, 2, 2));
        painelOpcoes.setBorder(BorderFactory.createTitledBorder("Caracteres"));
        chkMaiusculas = new JCheckBox("Letras maiusculas (A-Z)", true);
        chkMinusculas = new JCheckBox("Letras minusculas (a-z)", true);
        chkNumeros = new JCheckBox("Numeros (0-9)", true);
        chkSimbolos = new JCheckBox("Simbolos (!@#$...)", true);
        chkSemAmbiguos = new JCheckBox("Evitar ambiguos (l, 1, I, O, 0, o, |)", false);
        painelOpcoes.add(chkMaiusculas);
        painelOpcoes.add(chkMinusculas);
        painelOpcoes.add(chkNumeros);
        painelOpcoes.add(chkSimbolos);
        painelOpcoes.add(chkSemAmbiguos);

        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 2;
        painel.add(painelOpcoes, gbc);
        linha++;

        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 1;
        painel.add(new JLabel("Senha gerada:"), gbc);
        previsualizacao = new JTextField();
        previsualizacao.setEditable(false);
        previsualizacao.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        gbc.gridx = 1;
        painel.add(previsualizacao, gbc);
        linha++;

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton botaoRegenerar = new JButton("Regenerar");
        botaoRegenerar.addActionListener(e -> regenerar());
        JButton botaoCancelar = new JButton("Cancelar");
        botaoCancelar.addActionListener(e -> {
            confirmado = false;
            dispose();
        });
        JButton botaoUsar = new JButton("Usar");
        botaoUsar.addActionListener(e -> {
            confirmado = true;
            dispose();
        });
        painelBotoes.add(botaoRegenerar);
        painelBotoes.add(botaoCancelar);
        painelBotoes.add(botaoUsar);

        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 2;
        painel.add(painelBotoes, gbc);

        campoTamanho.addChangeListener(e -> regenerar());
        chkMaiusculas.getModel().addChangeListener(e -> regenerar());
        chkMinusculas.getModel().addChangeListener(e -> regenerar());
        chkNumeros.getModel().addChangeListener(e -> regenerar());
        chkSimbolos.getModel().addChangeListener(e -> regenerar());
        chkSemAmbiguos.getModel().addChangeListener(e -> regenerar());

        getRootPane().setDefaultButton(botaoUsar);
        setContentPane(painel);
        pack();
        setLocationRelativeTo(owner);
        regenerar();
    }

    private void regenerar() {
        try {
            int tamanho = (Integer) campoTamanho.getValue();
            String senha = PasswordGenerator.gerar(tamanho,
                    chkMaiusculas.isSelected(),
                    chkMinusculas.isSelected(),
                    chkNumeros.isSelected(),
                    chkSimbolos.isSelected(),
                    chkSemAmbiguos.isSelected());
            senhaGerada = senha;
            previsualizacao.setText(senha);
        } catch (IllegalArgumentException ex) {
            senhaGerada = "";
            previsualizacao.setText(ex.getMessage());
        }
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public String getSenha() {
        return senhaGerada;
    }
}
