package gerenciador.ui;

import gerenciador.model.CampoPersonalizado;
import gerenciador.model.PasswordEntry;
import gerenciador.util.PasswordGenerator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialogo modal para criar ou editar uma entrada de senha, com categoria,
 * campos personalizados, medidor de forca, gerador configuravel e historico
 * de senhas anteriores.
 */
public class EntryDialog extends JDialog {

    private static final String[] CATEGORIAS_SUGERIDAS = {
            "Geral", "E-mail", "Banco", "Redes sociais", "Trabalho",
            "Compras", "Streaming", "SSH", "Outro"
    };

    private JTextField campoSite;
    private JTextField campoUsuario;
    private JPasswordField campoSenha;
    private JComboBox<String> campoCategoria;
    private JTextField campoUrl;
    private JTextArea campoNotas;
    private JCheckBox mostrarSenha;
    private JLabel labelForca;
    private JTable tabelaCampos;
    private CamposModel modeloCampos;
    private JComboBox<String> comboHistorico;

    private boolean confirmado = false;
    private final PasswordEntry entrada;

    public EntryDialog(Window owner, PasswordEntry entradaExistente) {
        super(owner, entradaExistente == null ? "Nova entrada" : "Editar entrada", ModalityType.APPLICATION_MODAL);
        this.entrada = entradaExistente == null ? new PasswordEntry() : entradaExistente;
        montarInterface();
        preencherCampos();
        pack();
        setLocationRelativeTo(owner);
    }

    private void montarInterface() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int linha = 0;

        campoSite = new JTextField(24);
        adicionarCampo(painel, gbc, linha++, "Site / Servico:", campoSite);

        campoUsuario = new JTextField(24);
        adicionarCampo(painel, gbc, linha++, "Usuario / E-mail:", campoUsuario);

        campoSenha = new JPasswordField(24);
        adicionarCampo(painel, gbc, linha++, "Senha:", campoSenha);

        JPanel painelBotoesSenha = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        mostrarSenha = new JCheckBox("Mostrar");
        mostrarSenha.addActionListener(e ->
                campoSenha.setEchoChar(mostrarSenha.isSelected() ? (char) 0 : '\u2022'));
        JButton botaoGerar = new JButton("Gerar senha forte...");
        botaoGerar.addActionListener(e -> abrirGerador());
        painelBotoesSenha.add(mostrarSenha);
        painelBotoesSenha.add(botaoGerar);

        gbc.gridx = 1; gbc.gridy = linha;
        painel.add(painelBotoesSenha, gbc);
        linha++;

        labelForca = new JLabel(" ");
        labelForca.setFont(labelForca.getFont().deriveFont(Font.ITALIC, 11f));
        gbc.gridx = 1; gbc.gridy = linha++;
        painel.add(labelForca, gbc);

        campoCategoria = new JComboBox<>(CATEGORIAS_SUGERIDAS);
        campoCategoria.setEditable(true);
        adicionarCampo(painel, gbc, linha++, "Categoria:", campoCategoria);

        campoUrl = new JTextField(24);
        adicionarCampo(painel, gbc, linha++, "URL:", campoUrl);

        gbc.gridx = 0; gbc.gridy = linha;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        painel.add(new JLabel("Notas:"), gbc);
        campoNotas = new JTextArea(3, 24);
        campoNotas.setLineWrap(true);
        campoNotas.setWrapStyleWord(true);
        gbc.gridx = 1; gbc.gridy = linha++;
        gbc.anchor = GridBagConstraints.WEST;
        painel.add(new JScrollPane(campoNotas), gbc);

        // ---- Campos personalizados ----
        modeloCampos = new CamposModel(entrada.getCampos());
        tabelaCampos = new JTable(modeloCampos);
        tabelaCampos.setPreferredScrollableViewportSize(new Dimension(300, 90));
        tabelaCampos.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        JPanel painelCampos = new JPanel(new BorderLayout());
        JPanel botoesCampos = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton adicionarCampoBtn = new JButton("Adicionar campo");
        adicionarCampoBtn.addActionListener(e -> modeloCampos.adicionar("", ""));
        JButton removerCampoBtn = new JButton("Remover campo");
        removerCampoBtn.addActionListener(e -> modeloCampos.removerLinha(tabelaCampos.getSelectedRow()));
        botoesCampos.add(adicionarCampoBtn);
        botoesCampos.add(removerCampoBtn);
        painelCampos.add(botoesCampos, BorderLayout.NORTH);
        painelCampos.add(new JScrollPane(tabelaCampos), BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = linha;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        painel.add(new JLabel("Campos:"), gbc);
        gbc.gridx = 1; gbc.gridy = linha++;
        gbc.anchor = GridBagConstraints.WEST;
        painel.add(painelCampos, gbc);

        // ---- Historico ----
        comboHistorico = new JComboBox<>();
        comboHistorico.setMaximumRowCount(8);
        JPanel painelHistorico = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        painelHistorico.add(new JLabel("Historico:"));
        painelHistorico.add(comboHistorico);
        JButton restaurarBtn = new JButton("Restaurar");
        restaurarBtn.addActionListener(e -> restaurarSenhaHistorica());
        painelHistorico.add(restaurarBtn);
        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 2;
        painel.add(painelHistorico, gbc);
        linha++;
        gbc.gridwidth = 1;

        campoSenha.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { atualizarForca(); }
            @Override public void removeUpdate(DocumentEvent e) { atualizarForca(); }
            @Override public void changedUpdate(DocumentEvent e) { atualizarForca(); }
        });

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton botaoCancelar = new JButton("Cancelar");
        botaoCancelar.addActionListener(e -> {
            confirmado = false;
            dispose();
        });
        JButton botaoSalvar = new JButton("Salvar");
        botaoSalvar.addActionListener(e -> salvar());
        painelBotoes.add(botaoCancelar);
        painelBotoes.add(botaoSalvar);

        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 2;
        painel.add(painelBotoes, gbc);

        getRootPane().setDefaultButton(botaoSalvar);
        setContentPane(painel);
    }

    private void adicionarCampo(JPanel painel, GridBagConstraints gbc, int linha, String rotulo, JComponent campo) {
        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        painel.add(new JLabel(rotulo), gbc);
        gbc.gridx = 1;
        painel.add(campo, gbc);
    }

    private void preencherCampos() {
        campoSite.setText(nulo(entrada.getSite()));
        campoUsuario.setText(nulo(entrada.getUsuario()));
        campoSenha.setText(nulo(entrada.getSenha()));

        String categoria = entrada.getCategoria();
        campoCategoria.setSelectedItem(categoria == null || categoria.isEmpty() ? null : categoria);

        campoUrl.setText(nulo(entrada.getUrl()));
        campoNotas.setText(nulo(entrada.getNotas()));

        modeloCampos.atualizar(entrada.getCampos());

        for (String antiga : entrada.getHistoricoSenhas()) {
            comboHistorico.addItem(antiga);
        }
        atualizarForca();
    }

    private void abrirGerador() {
        PasswordGeneratorDialog dialog = new PasswordGeneratorDialog(this);
        dialog.setVisible(true);
        if (dialog.isConfirmado()) {
            campoSenha.setText(dialog.getSenha());
        }
    }

    private void restaurarSenhaHistorica() {
        String selecionada = (String) comboHistorico.getSelectedItem();
        if (selecionada == null) return;
        campoSenha.setText(selecionada);
    }

    private void atualizarForca() {
        String senha = new String(campoSenha.getPassword());
        if (senha.isEmpty()) {
            labelForca.setText(" ");
            labelForca.setForeground(getForeground());
            return;
        }
        double entropia = PasswordGenerator.entropia(senha);
        String rotulo = PasswordGenerator.rotuloForca(entropia);
        labelForca.setText("Forca: " + rotulo + " (~" + (int) entropia + " bits)");
        if (entropia < 50) labelForca.setForeground(new Color(0xC62828));
        else if (entropia < 80) labelForca.setForeground(new Color(0xF9A825));
        else if (entropia < 120) labelForca.setForeground(new Color(0x2E7D32));
        else labelForca.setForeground(new Color(0x00695C));
    }

    private void salvar() {
        if (campoSite.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe o nome do site/servico.",
                    "Campo obrigatorio", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String novaSenha = new String(campoSenha.getPassword());
        if (!novaSenha.equals(nulo(entrada.getSenha()))) {
            entrada.arquivarSenhaAtual();
        }
        entrada.setSite(campoSite.getText().trim());
        entrada.setUsuario(campoUsuario.getText().trim());
        entrada.setSenha(novaSenha);
        Object categoria = campoCategoria.getSelectedItem();
        entrada.setCategoria(categoria == null ? "" : categoria.toString().trim());
        entrada.setUrl(campoUrl.getText().trim());
        entrada.setNotas(campoNotas.getText());
        entrada.setCampos(modeloCampos.obterCampos());
        confirmado = true;
        dispose();
    }

    private static String nulo(String s) {
        return s == null ? "" : s;
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public PasswordEntry getEntrada() {
        return entrada;
    }

    // ------------------------------------------------------------
    // Modelo dos campos personalizados
    // ------------------------------------------------------------

    private class CamposModel extends AbstractTableModel {
        private final String[] colunas = {"Campo", "Valor"};
        private final List<CampoPersonalizado> campos;

        CamposModel(List<CampoPersonalizado> campos) {
            this.campos = campos == null ? new ArrayList<>() : campos;
        }

        void atualizar(List<CampoPersonalizado> campos) {
            this.campos.clear();
            if (campos != null) this.campos.addAll(campos);
            fireTableDataChanged();
        }

        void adicionar(String nome, String valor) {
            campos.add(new CampoPersonalizado(nome, valor));
            fireTableDataChanged();
        }

        void removerLinha(int linha) {
            if (linha >= 0 && linha < campos.size()) {
                campos.remove(linha);
                fireTableDataChanged();
            }
        }

        List<CampoPersonalizado> obterCampos() {
            List<CampoPersonalizado> resultado = new ArrayList<>();
            for (CampoPersonalizado c : campos) {
                String nome = c.getNome() == null ? "" : c.getNome().trim();
                if (!nome.isEmpty() || (c.getValor() != null && !c.getValor().isEmpty())) {
                    resultado.add(new CampoPersonalizado(nome, nulo(c.getValor())));
                }
            }
            return resultado;
        }

        @Override public int getRowCount() { return campos.size(); }
        @Override public int getColumnCount() { return colunas.length; }
        @Override public String getColumnName(int column) { return colunas[column]; }
        @Override public boolean isCellEditable(int row, int col) { return true; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            CampoPersonalizado c = campos.get(rowIndex);
            return columnIndex == 0 ? c.getNome() : c.getValor();
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            CampoPersonalizado c = campos.get(rowIndex);
            if (columnIndex == 0) c.setNome(aValue == null ? "" : aValue.toString());
            else c.setValor(aValue == null ? "" : aValue.toString());
        }
    }
}
