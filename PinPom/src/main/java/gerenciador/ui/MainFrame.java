package gerenciador.ui;

import gerenciador.model.Identidade;
import gerenciador.model.PasswordEntry;
import gerenciador.model.VaultData;
import gerenciador.storage.VaultManager;
import gerenciador.util.AppInfo;
import gerenciador.util.ConfigStore;
import gerenciador.util.PasswordGenerator;
import gerenciador.util.SystemUtil;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Janela principal: senhas em grade de cartoes (cards) e identidades em
 * tabela, toolbar superior estilizada, menu, atalhos, tema claro/escuro,
 * bloqueio por inatividade, limpeza de clipboard, verificacao de senhas e
 * backup/restauracao do cofre.
 */
public class MainFrame extends JFrame {

    private final VaultManager vaultManager;
    private VaultData dados;
    private final IdentidadesModel modeloIdentidades;
    private final JTable tabelaIdentidades;
    private final JTextField campoBusca;
    private final JComboBox<String> comboCategoria;
    private final JComboBox<String> comboOrdenar;
    private final JCheckBox checkMostrarSenhas;
    private final JLabel labelRodape;
    private final JPanel painelCards;
    private final JScrollPane painelCardsScroll;
    private PasswordEntry selecionada = null;
    private boolean senhasVisiveis = false;
    private final Map<PasswordEntry, PasswordCard> cartaoPorEntrada = new HashMap<>();

    private Timer autoLockTimer;
    private java.awt.event.AWTEventListener awtListener;

    public MainFrame(VaultManager vaultManager, VaultData dadosIniciais) {
        super(AppInfo.NOME + " - " + vaultManager.getArquivoCofre());
        this.vaultManager = vaultManager;
        this.dados = dadosIniciais;
        this.modeloIdentidades = new IdentidadesModel();
        this.tabelaIdentidades = criarTabela(modeloIdentidades);
        this.campoBusca = new JTextField(18);
        this.comboCategoria = new JComboBox<>();
        this.comboOrdenar = new JComboBox<>(new String[]{
                "Ordenar por: Site", "Ordenar por: Categoria", "Ordenar por: Usuario"});
        this.checkMostrarSenhas = new JCheckBox("Mostrar senhas");
        this.painelCards = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 14));
        this.painelCardsScroll = new JScrollPane(painelCards);
        this.painelCardsScroll.setBorder(null);
        this.painelCardsScroll.getVerticalScrollBar().setUnitIncrement(16);
        this.labelRodape = new JLabel("  " + AppInfo.NOME_CURTO + " | " + dados.getEntradas().size()
                + " senha(s), " + dados.getIdentidades().size() + " identidade(s)");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1020, 600);
        setLocationRelativeTo(null);
        montarInterface();
        montarAtalhos();
        configurarSeguranca();
        iniciarAutoLock();
        atualizarCategorias();
        rebuildCards();
    }

    private JTable criarTabela(AbstractTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setRowHeight(26);
        t.setAutoCreateRowSorter(false);
        return t;
    }

    // ------------------------------------------------------------
    // Interface
    // ------------------------------------------------------------

    private void montarInterface() {
        setLayout(new BorderLayout());

        setJMenuBar(criarMenu());
        add(montarBarraFerramentas(), BorderLayout.NORTH);

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Senhas", montarPainelSenhas());
        abas.addTab("Identidades", montarPainelIdentidades());
        abas.addChangeListener(e -> atualizarRodape());
        add(abas, BorderLayout.CENTER);

        labelRodape.setFont(labelRodape.getFont().deriveFont(11f));
        add(labelRodape, BorderLayout.SOUTH);
    }

    private JComponent montarBarraFerramentas() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        toolbar.add(botao(UiUtil.botaoPrimario("Adicionar"), e -> adicionar()));
        toolbar.add(UiUtil.espaco(2));
        toolbar.add(botao(UiUtil.botaoSecundario("Editar"), e -> editar(obterSelecionada())));
        toolbar.add(UiUtil.espaco(2));
        toolbar.add(botao(UiUtil.botaoSecundario("Duplicar"), e -> duplicar(obterSelecionada())));
        toolbar.add(UiUtil.espaco(2));
        toolbar.add(botao(UiUtil.botaoSecundario("Remover"), e -> remover(obterSelecionada())));
        toolbar.addSeparator();

        toolbar.add(botao(UiUtil.botaoSecundario("Copiar senha"), e -> copiarSenha(obterSelecionada())));
        toolbar.add(UiUtil.espaco(2));
        toolbar.add(botao(UiUtil.botaoSecundario("Abrir URL"), e -> abrirUrl(obterSelecionada())));
        toolbar.addSeparator();

        toolbar.add(botao(UiUtil.botaoSecundario("Verificar senhas"), e -> verificarSenhas()));
        toolbar.addSeparator();

        toolbar.add(botao(UiUtil.botaoSecundario("Importar CSV"), e -> importarCsv()));
        toolbar.add(UiUtil.espaco(2));
        toolbar.add(botao(UiUtil.botaoSecundario("Exportar CSV"), e -> exportarCsv()));
        toolbar.addSeparator();

        toolbar.add(botao(UiUtil.botaoSecundario("Backup"), e -> fazerBackup()));
        toolbar.add(UiUtil.espaco(2));
        toolbar.add(botao(UiUtil.botaoSecundario("Restaurar"), e -> restaurarBackup()));
        toolbar.addSeparator();

        toolbar.add(botao(UiUtil.botaoSecundario("Configuracoes"), e -> abrirConfiguracoes()));
        toolbar.add(UiUtil.espaco(2));
        toolbar.add(botao(UiUtil.botaoSecundario("Sobre"), e -> abrirSobre()));

        return toolbar;
    }

    private JButton botao(JButton b, java.awt.event.ActionListener acao) {
        b.addActionListener(acao);
        return b;
    }

    private JPanel montarPainelSenhas() {
        JPanel painel = new JPanel(new BorderLayout());

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filtros.add(new JLabel("Buscar:"));
        filtros.add(campoBusca);
        filtros.add(new JLabel("Categoria:"));
        filtros.add(comboCategoria);
        filtros.add(new JLabel("Ordenar:"));
        filtros.add(comboOrdenar);
        checkMostrarSenhas.addActionListener(e -> {
            senhasVisiveis = checkMostrarSenhas.isSelected();
            rebuildCards();
        });
        filtros.add(checkMostrarSenhas);
        painel.add(filtros, BorderLayout.NORTH);

        campoBusca.addCaretListener(e -> rebuildCards());
        comboCategoria.addActionListener(e -> rebuildCards());
        comboOrdenar.addActionListener(e -> rebuildCards());

        painel.add(painelCardsScroll, BorderLayout.CENTER);
        return painel;
    }

    private JPanel montarPainelIdentidades() {
        JPanel painel = new JPanel(new BorderLayout());

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 10));
        JButton adicionar = UiUtil.botaoPrimario("Adicionar");
        adicionar.addActionListener(e -> adicionarIdentidade());
        JButton editar = UiUtil.botaoSecundario("Editar");
        editar.addActionListener(e -> editarIdentidadeSelecionada());
        JButton remover = UiUtil.botaoSecundario("Remover");
        remover.addActionListener(e -> removerIdentidadeSelecionada());
        JButton detalhes = UiUtil.botaoSecundario("Detalhes");
        detalhes.addActionListener(e -> detalhesIdentidadeSelecionada());
        topo.add(adicionar);
        topo.add(UiUtil.espaco(2));
        topo.add(editar);
        topo.add(UiUtil.espaco(2));
        topo.add(remover);
        topo.add(UiUtil.espaco(2));
        topo.add(detalhes);
        painel.add(topo, BorderLayout.NORTH);

        tabelaIdentidades.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editarIdentidadeSelecionada();
            }
        });

        painel.add(new JScrollPane(tabelaIdentidades), BorderLayout.CENTER);
        return painel;
    }

    private JMenuBar criarMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu arquivo = new JMenu("Arquivo");
        arquivo.add(menuItem("Nova entrada", KeyStroke.getKeyStroke("control N"), e -> adicionar()));
        arquivo.addSeparator();
        arquivo.add(menuItem("Importar CSV...", null, e -> importarCsv()));
        arquivo.add(menuItem("Exportar CSV...", null, e -> exportarCsv()));
        arquivo.addSeparator();
        arquivo.add(menuItem("Fazer backup...", null, e -> fazerBackup()));
        arquivo.add(menuItem("Restaurar backup...", null, e -> restaurarBackup()));
        arquivo.addSeparator();
        arquivo.add(menuItem("Alterar senha mestre...", null, e -> alterarSenhaMestre()));
        arquivo.addSeparator();
        arquivo.add(menuItem("Bloquear", KeyStroke.getKeyStroke("control L"), e -> bloquear()));
        arquivo.add(menuItem("Sair", null, e -> dispose()));
        menuBar.add(arquivo);

        JMenu editar = new JMenu("Editar");
        editar.add(menuItem("Editar entrada", KeyStroke.getKeyStroke("F2"), e -> editar(obterSelecionada())));
        editar.add(menuItem("Remover entrada", KeyStroke.getKeyStroke("DELETE"), e -> remover(obterSelecionada())));
        editar.add(menuItem("Copiar senha", KeyStroke.getKeyStroke("control C"), e -> copiarSenha(obterSelecionada())));
        editar.add(menuItem("Duplicar entrada", KeyStroke.getKeyStroke("control D"), e -> duplicar(obterSelecionada())));
        editar.add(menuItem("Abrir URL", null, e -> abrirUrl(obterSelecionada())));
        menuBar.add(editar);

        JMenu ferramentas = new JMenu("Ferramentas");
        ferramentas.add(menuItem("Verificar senhas", null, e -> verificarSenhas()));
        ferramentas.add(menuItem("Configuracoes", KeyStroke.getKeyStroke("control COMMA"), e -> abrirConfiguracoes()));
        menuBar.add(ferramentas);

        JMenu ajuda = new JMenu("Ajuda");
        JCheckBoxMenuItem itemTema = new JCheckBoxMenuItem("Tema escuro", ConfigStore.isTemaEscuro());
        itemTema.addActionListener(e -> alternarTema(itemTema.isSelected()));
        itemTema.setAccelerator(KeyStroke.getKeyStroke("control T"));
        ajuda.add(itemTema);
        ajuda.addSeparator();
        ajuda.add(menuItem("Ajuda", KeyStroke.getKeyStroke("F1"), e -> abrirAjuda()));
        ajuda.add(menuItem("Sobre o criador", null, e -> abrirSobre()));
        menuBar.add(ajuda);

        return menuBar;
    }

    private JMenuItem menuItem(String texto, KeyStroke atalho, java.awt.event.ActionListener acao) {
        JMenuItem item = new JMenuItem(texto);
        if (atalho != null) item.setAccelerator(atalho);
        item.addActionListener(acao);
        return item;
    }

    private void montarAtalhos() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();
        inputMap.put(KeyStroke.getKeyStroke("control F"), "focarBusca");
        actionMap.put("focarBusca", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                campoBusca.requestFocusInWindow();
            }
        });
    }

    // ------------------------------------------------------------
    // Seguranca: ocultar senhas ao perder o foco e auto-lock
    // ------------------------------------------------------------

    private void configurarSeguranca() {
        if (!ConfigStore.isCapturaPermitida()) {
            addWindowFocusListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowLostFocus(WindowEvent e) {
                    ocultarSenhas();
                }
            });
        }
    }

    private void ocultarSenhas() {
        if (senhasVisiveis) {
            senhasVisiveis = false;
            checkMostrarSenhas.setSelected(false);
            rebuildCards();
        }
    }

    private void iniciarAutoLock() {
        int minutos = ConfigStore.getBloqueioMinutos();
        if (minutos <= 0) return;
        autoLockTimer = new Timer(minutos * 60_000, e -> bloquear());
        autoLockTimer.start();
        awtListener = event -> {
            if (autoLockTimer != null) autoLockTimer.restart();
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(awtListener,
                java.awt.AWTEvent.MOUSE_EVENT_MASK
                        | java.awt.AWTEvent.MOUSE_MOTION_EVENT_MASK
                        | java.awt.AWTEvent.KEY_EVENT_MASK);
    }

    @Override
    public void dispose() {
        if (autoLockTimer != null) autoLockTimer.stop();
        if (awtListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(awtListener);
        }
        super.dispose();
    }

    // ------------------------------------------------------------
    // Filtros, ordenacao e grade de cartoes
    // ------------------------------------------------------------

    private void rebuildCards() {
        painelCards.removeAll();
        cartaoPorEntrada.clear();

        List<PasswordEntry> visiveis = filtrarEOrdenar();
        if (visiveis.isEmpty()) {
            JLabel vazio = new JLabel("Nenhuma entrada encontrada.");
            vazio.setFont(vazio.getFont().deriveFont(Font.ITALIC, 13f));
            vazio.setForeground(UiUtil.cor("Label.disabledForeground", new Color(0x9E9E9E)));
            vazio.setBorder(BorderFactory.createEmptyBorder(40, 20, 0, 0));
            painelCards.add(vazio);
        } else {
            for (PasswordEntry e : visiveis) {
                PasswordCard card = new PasswordCard(e, ConfigStore.isIconesSite(), senhasVisiveis,
                        () -> selecionar(e),
                        () -> copiarSenha(e),
                        () -> editar(e),
                        () -> abrirUrl(e),
                        () -> remover(e));
                card.setSelecionada(e == selecionada);
                cartaoPorEntrada.put(e, card);
                painelCards.add(card);
            }
        }
        painelCards.revalidate();
        painelCards.repaint();
    }

    private List<PasswordEntry> filtrarEOrdenar() {
        String texto = campoBusca.getText().trim();
        String categoria = (String) comboCategoria.getSelectedItem();

        List<PasswordEntry> resultado = new ArrayList<>();
        for (PasswordEntry e : dados.getEntradas()) {
            if (!texto.isEmpty() && !contem(e, texto)) continue;
            if (categoria != null && !categoria.isEmpty()
                    && !categoria.equals("Todas as categorias")
                    && !categoria.equalsIgnoreCase(nulo(e.getCategoria()))) {
                continue;
            }
            resultado.add(e);
        }

        Comparator<PasswordEntry> comp;
        switch (comboOrdenar.getSelectedIndex()) {
            case 1 -> comp = Comparator.comparing((PasswordEntry e) -> nulo(e.getCategoria()),
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(e -> nulo(e.getSite()), String.CASE_INSENSITIVE_ORDER);
            case 2 -> comp = Comparator.comparing((PasswordEntry e) -> nulo(e.getUsuario()),
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(e -> nulo(e.getSite()), String.CASE_INSENSITIVE_ORDER);
            default -> comp = Comparator.comparing((PasswordEntry e) -> nulo(e.getSite()),
                    String.CASE_INSENSITIVE_ORDER);
        }
        resultado.sort(comp);
        return resultado;
    }

    private boolean contem(PasswordEntry e, String texto) {
        String t = texto.toLowerCase();
        return nulo(e.getSite()).toLowerCase().contains(t)
                || nulo(e.getUsuario()).toLowerCase().contains(t)
                || nulo(e.getSenha()).toLowerCase().contains(t)
                || nulo(e.getUrl()).toLowerCase().contains(t)
                || nulo(e.getNotas()).toLowerCase().contains(t)
                || nulo(e.getCategoria()).toLowerCase().contains(t);
    }

    private void selecionar(PasswordEntry e) {
        selecionada = e;
        for (Map.Entry<PasswordEntry, PasswordCard> me : cartaoPorEntrada.entrySet()) {
            me.getValue().setSelecionada(me.getKey() == selecionada);
        }
    }

    private void rolarAte(PasswordEntry e) {
        PasswordCard card = cartaoPorEntrada.get(e);
        if (card == null) return;
        SwingUtilities.invokeLater(() -> {
            Rectangle r = card.getBounds();
            painelCardsScroll.getViewport().setViewPosition(
                    new Point(Math.max(0, r.x - 10), Math.max(0, r.y - 10)));
        });
    }

    private void atualizarCategorias() {
        String selecionada = (String) comboCategoria.getSelectedItem();
        Set<String> categorias = new HashSet<>();
        for (PasswordEntry e : dados.getEntradas()) {
            if (e.getCategoria() != null && !e.getCategoria().isBlank()) {
                categorias.add(e.getCategoria());
            }
        }
        List<String> lista = new ArrayList<>(categorias);
        lista.sort(Comparator.naturalOrder());
        lista.add(0, "Todas as categorias");

        comboCategoria.setModel(new DefaultComboBoxModel<>(lista.toArray(new String[0])));
        if (selecionada != null && lista.contains(selecionada)) {
            comboCategoria.setSelectedItem(selecionada);
        }
    }

    private void atualizarRodape() {
        labelRodape.setText("  " + AppInfo.NOME_CURTO + " | " + dados.getEntradas().size()
                + " senha(s), " + dados.getIdentidades().size() + " identidade(s)");
    }

    private void mostrarStatus(String mensagem) {
        labelRodape.setText("  " + mensagem);
        Timer timer = new Timer(4000, e -> atualizarRodape());
        timer.setRepeats(false);
        timer.start();
    }

    // ------------------------------------------------------------
    // Acoes de senhas
    // ------------------------------------------------------------

    private void adicionar() {
        EntryDialog dialog = new EntryDialog(this, null);
        dialog.setVisible(true);
        if (dialog.isConfirmado()) {
            PasswordEntry nova = dialog.getEntrada();
            dados.getEntradas().add(nova);
            selecionada = nova;
            atualizarCategorias();
            atualizarRodape();
            persistir();
            rebuildCards();
            rolarAte(nova);
        }
    }

    private void editar(PasswordEntry entrada) {
        if (entrada == null) return;
        EntryDialog dialog = new EntryDialog(this, entrada);
        dialog.setVisible(true);
        if (dialog.isConfirmado()) {
            selecionada = entrada;
            atualizarCategorias();
            atualizarRodape();
            persistir();
            rebuildCards();
            rolarAte(entrada);
        }
    }

    private void duplicar(PasswordEntry original) {
        if (original == null) return;
        PasswordEntry copia = new PasswordEntry();
        copia.setSite(original.getSite());
        copia.setUsuario(original.getUsuario());
        copia.setSenha(original.getSenha());
        copia.setUrl(original.getUrl());
        copia.setNotas(original.getNotas());
        copia.setCategoria(original.getCategoria());
        for (var campo : original.getCampos()) {
            copia.getCampos().add(new gerenciador.model.CampoPersonalizado(campo.getNome(), campo.getValor()));
        }
        dados.getEntradas().add(copia);
        selecionada = copia;
        atualizarCategorias();
        atualizarRodape();
        persistir();
        rebuildCards();
        rolarAte(copia);
        mostrarStatus("Entrada duplicada: " + copia.getSite());
    }

    private void remover(PasswordEntry entrada) {
        if (entrada == null) return;
        int resp = JOptionPane.showConfirmDialog(this,
                "Remover a entrada \"" + entrada.getSite() + "\"?",
                "Confirmar remocao", JOptionPane.YES_NO_OPTION);
        if (resp == JOptionPane.YES_OPTION) {
            dados.getEntradas().remove(entrada);
            if (selecionada == entrada) selecionada = null;
            atualizarCategorias();
            atualizarRodape();
            persistir();
            rebuildCards();
        }
    }

    private void copiarSenha(PasswordEntry entrada) {
        if (entrada == null) return;
        selecionar(entrada);
        StringSelection selecao = new StringSelection(entrada.getSenha());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selecao, null);
        mostrarStatus("Senha de \"" + entrada.getSite() + "\" copiada.");

        int segundos = ConfigStore.getClipboardSegundos();
        if (segundos > 0) {
            Timer timer = new Timer(segundos * 1000, e ->
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(new StringSelection(""), null));
            timer.setRepeats(false);
            timer.start();
        }
    }

    private void abrirUrl(PasswordEntry entrada) {
        if (entrada == null) return;
        selecionar(entrada);
        String url = entrada.getUrl();
        if (url == null || url.isBlank()) {
            JOptionPane.showMessageDialog(this, "Esta entrada nao possui URL cadastrada.",
                    "Abrir URL", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        SystemUtil.abrirUrl(this, url);
    }

    private PasswordEntry obterSelecionada() {
        if (selecionada == null) {
            JOptionPane.showMessageDialog(this, "Selecione uma entrada na grade.",
                    "Nenhuma selecao", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return selecionada;
    }

    // ------------------------------------------------------------
    // Identidades
    // ------------------------------------------------------------

    private void adicionarIdentidade() {
        IdentidadeDialog dialog = new IdentidadeDialog(this, null);
        dialog.setVisible(true);
        if (dialog.isConfirmado()) {
            dados.getIdentidades().add(dialog.getIdentidade());
            modeloIdentidades.fireTableDataChanged();
            atualizarRodape();
            persistir();
        }
    }

    private void editarIdentidadeSelecionada() {
        Identidade identidade = obterIdentidadeSelecionada();
        if (identidade == null) return;
        IdentidadeDialog dialog = new IdentidadeDialog(this, identidade);
        dialog.setVisible(true);
        if (dialog.isConfirmado()) {
            modeloIdentidades.fireTableDataChanged();
            persistir();
        }
    }

    private void removerIdentidadeSelecionada() {
        Identidade identidade = obterIdentidadeSelecionada();
        if (identidade == null) return;
        int resp = JOptionPane.showConfirmDialog(this,
                "Remover a identidade \"" + identidade.getNome() + "\"?",
                "Confirmar remocao", JOptionPane.YES_NO_OPTION);
        if (resp == JOptionPane.YES_OPTION) {
            dados.getIdentidades().remove(identidade);
            modeloIdentidades.fireTableDataChanged();
            atualizarRodape();
            persistir();
        }
    }

    private void detalhesIdentidadeSelecionada() {
        Identidade identidade = obterIdentidadeSelecionada();
        if (identidade == null) return;
        IdentidadeDialog dialog = new IdentidadeDialog(this, identidade);
        dialog.setVisible(true);
        if (dialog.isConfirmado()) {
            modeloIdentidades.fireTableDataChanged();
            persistir();
        }
    }

    private Identidade obterIdentidadeSelecionada() {
        int linha = tabelaIdentidades.getSelectedRow();
        if (linha < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma identidade na tabela.",
                    "Nenhuma selecao", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int linhaModelo = tabelaIdentidades.convertRowIndexToModel(linha);
        return dados.getIdentidades().get(linhaModelo);
    }

    // ------------------------------------------------------------
    // Verificacao de senhas
    // ------------------------------------------------------------

    private void verificarSenhas() {
        Map<String, List<PasswordEntry>> porSenha = new HashMap<>();
        List<PasswordEntry> fracas = new ArrayList<>();
        for (PasswordEntry e : dados.getEntradas()) {
            String senha = e.getSenha();
            if (senha != null && !senha.isEmpty()) {
                porSenha.computeIfAbsent(senha, k -> new ArrayList<>()).add(e);
            }
            if (PasswordGenerator.entropia(senha == null ? "" : senha) < 50) {
                fracas.add(e);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== SENHAS REUTILIZADAS ===\n");
        boolean algumaReutilizada = false;
        for (Map.Entry<String, List<PasswordEntry>> me : porSenha.entrySet()) {
            if (me.getValue().size() > 1) {
                algumaReutilizada = true;
                sb.append("\nSenha usada em ").append(me.getValue().size()).append(" entradas:\n");
                for (PasswordEntry e : me.getValue()) {
                    sb.append("  - ").append(e.getSite()).append(" (").append(e.getUsuario()).append(")\n");
                }
            }
        }
        if (!algumaReutilizada) sb.append("Nenhuma senha reutilizada. Bom trabalho!\n");

        sb.append("\n\n=== SENHAS FRACAS (entropia < 50 bits) ===\n");
        if (fracas.isEmpty()) {
            sb.append("Nenhuma senha fraca encontrada.\n");
        } else {
            for (PasswordEntry e : fracas) {
                double entropia = PasswordGenerator.entropia(e.getSenha());
                sb.append("  - ").append(e.getSite()).append(" (").append((int) entropia)
                        .append(" bits)\n");
            }
        }

        JTextArea area = new JTextArea(sb.toString(), 22, 60);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(620, 380));
        JOptionPane.showMessageDialog(this, scroll, "Verificacao de senhas",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ------------------------------------------------------------
    // Backup / restauracao
    // ------------------------------------------------------------

    private void fazerBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Salvar backup do cofre (arquivo cifrado)");
        chooser.setSelectedFile(new File("pinpom-backup.dat"));
        int resp = chooser.showSaveDialog(this);
        if (resp != JFileChooser.APPROVE_OPTION) return;
        try {
            vaultManager.fazerBackup(chooser.getSelectedFile());
            JOptionPane.showMessageDialog(this,
                    "Backup salvo em:\n" + chooser.getSelectedFile().getAbsolutePath(),
                    "Backup", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao fazer backup: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void restaurarBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecionar arquivo de backup");
        int resp = chooser.showOpenDialog(this);
        if (resp != JFileChooser.APPROVE_OPTION) return;

        int aviso = JOptionPane.showConfirmDialog(this,
                "O backup substituira TODOS os dados atuais do cofre.\n\nContinuar?",
                "Restaurar backup", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (aviso != JOptionPane.YES_OPTION) return;

        try {
            VaultData novos = vaultManager.restaurarBackup(chooser.getSelectedFile());
            this.dados = novos;
            this.selecionada = null;
            modeloIdentidades.fireTableDataChanged();
            atualizarCategorias();
            atualizarRodape();
            rebuildCards();
            JOptionPane.showMessageDialog(this, "Backup restaurado com sucesso.",
                    "Restaurar backup", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Nao foi possivel restaurar: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------
    // Importacao / exportacao CSV
    // ------------------------------------------------------------

    private void importarCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecionar arquivo CSV para importar");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Arquivos CSV", "csv"));
        int resp = chooser.showOpenDialog(this);
        if (resp != JFileChooser.APPROVE_OPTION) return;

        try {
            List<PasswordEntry> importadas = vaultManager.importarCsv(chooser.getSelectedFile());
            if (importadas.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhuma entrada encontrada no arquivo.",
                        "Importar CSV", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            dados.getEntradas().addAll(importadas);
            atualizarCategorias();
            atualizarRodape();
            persistir();
            rebuildCards();
            JOptionPane.showMessageDialog(this, importadas.size() + " entrada(s) importada(s) com sucesso.",
                    "Importar CSV", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao importar CSV: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportarCsv() {
        if (dados.getEntradas().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nao ha entradas para exportar.",
                    "Exportar CSV", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int aviso = JOptionPane.showConfirmDialog(this,
                "O arquivo CSV exportado contera as senhas em TEXTO PURO (sem criptografia).\n"
                        + "Guarde-o em local seguro e apague-o quando nao precisar mais.\n\nContinuar?",
                "Aviso de seguranca", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (aviso != JOptionPane.YES_OPTION) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Salvar CSV de exportacao");
        chooser.setSelectedFile(new File("senhas_export.csv"));
        int resp = chooser.showSaveDialog(this);
        if (resp != JFileChooser.APPROVE_OPTION) return;

        File destino = chooser.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".csv")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".csv");
        }
        try {
            vaultManager.exportarCsv(destino, dados.getEntradas());
            JOptionPane.showMessageDialog(this, "Exportado com sucesso para:\n" + destino.getAbsolutePath(),
                    "Exportar CSV", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar CSV: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------
    // Outras acoes
    // ------------------------------------------------------------

    private void alterarSenhaMestre() {
        JPasswordField novaSenha = new JPasswordField();
        JPasswordField confirmacao = new JPasswordField();
        JPanel painel = new JPanel(new GridLayout(0, 1, 4, 4));
        painel.add(new JLabel("Nova senha mestre:"));
        painel.add(novaSenha);
        painel.add(new JLabel("Confirmar nova senha:"));
        painel.add(confirmacao);

        int resp = JOptionPane.showConfirmDialog(this, painel, "Alterar senha mestre",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resp != JOptionPane.OK_OPTION) return;

        char[] s1 = novaSenha.getPassword();
        char[] s2 = confirmacao.getPassword();
        try {
            if (s1.length < 8) {
                JOptionPane.showMessageDialog(this, "A senha deve ter pelo menos 8 caracteres.",
                        "Senha fraca", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!Arrays.equals(s1, s2)) {
                JOptionPane.showMessageDialog(this, "As senhas nao coincidem.",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            vaultManager.alterarSenhaMestre(s1, dados);
            JOptionPane.showMessageDialog(this, "Senha mestre alterada com sucesso.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar cofre: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(s1, '\0');
            Arrays.fill(s2, '\0');
        }
    }

    private void bloquear() {
        LoginFrame login = new LoginFrame();
        login.setVisible(true);
        dispose();
    }

    private void abrirConfiguracoes() {
        new SettingsDialog(this, vaultManager).setVisible(true);
    }

    private void alternarTema(boolean escuro) {
        ConfigStore.salvarTemaEscuro(escuro);
        ThemeUtil.aplicarTema(escuro);
        ThemeUtil.atualizarJanela(this);
        rebuildCards();
        mostrarStatus(escuro ? "Tema escuro ativado." : "Tema claro ativado.");
    }

    private void abrirAjuda() {
        new HelpDialog(this).setVisible(true);
    }

    private void abrirSobre() {
        new AboutDialog(this).setVisible(true);
    }

    private void persistir() {
        try {
            vaultManager.salvar(dados);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar o cofre: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------
    // Modelo da tabela de identidades
    // ------------------------------------------------------------

    private class IdentidadesModel extends AbstractTableModel {
        private final String[] colunas = {"Nome", "CPF", "Telefone", "E-mail"};

        @Override public int getRowCount() { return dados.getIdentidades().size(); }
        @Override public int getColumnCount() { return colunas.length; }
        @Override public String getColumnName(int column) { return colunas[column]; }
        @Override public boolean isCellEditable(int row, int col) { return false; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Identidade i = dados.getIdentidades().get(rowIndex);
            switch (columnIndex) {
                case 0: return i.getNome();
                case 1: return i.getCpf();
                case 2: return i.getTelefone();
                case 3: return i.getEmail();
                default: return "";
            }
        }
    }

    private static String nulo(String s) {
        return s == null ? "" : s;
    }
}
