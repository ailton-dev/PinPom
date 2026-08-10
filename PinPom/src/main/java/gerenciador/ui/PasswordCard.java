package gerenciador.ui;

import gerenciador.model.PasswordEntry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Cartao arredondado que representa uma entrada de senha na visao em grade.
 * Mostra o badge colorido do site, o nome, o usuario, a senha (mascarada ou
 * nao) e botoes de acao (copiar/editar/abrir/excluir). Suporta selecao e
 * acompanha o tema claro/escuro.
 */
public class PasswordCard extends JPanel {

    private static final Color[] CORES = {
            new Color(0x1E88E5), new Color(0x43A047), new Color(0xE53935),
            new Color(0x8E24AA), new Color(0xFB8C00), new Color(0x00ACC1),
            new Color(0x6D4C41), new Color(0x3949AB)
    };

    private static final int LARGURA = 285;
    private static final int ALTURA = 172;

    private final PasswordEntry entrada;
    private final boolean mostrarIcone;
    private boolean senhaVisivel;
    private boolean selecionada;
    private JLabel labelSenha;

    public PasswordCard(PasswordEntry entrada, boolean mostrarIcone, boolean senhaVisivel,
                        Runnable aoSelecionar, Runnable aoCopiar, Runnable aoEditar,
                        Runnable aoAbrir, Runnable aoRemover) {
        this.entrada = entrada;
        this.mostrarIcone = mostrarIcone;
        this.senhaVisivel = senhaVisivel;

        setOpaque(false);
        setBorder(new EmptyBorder(18, 16, 14, 16));
        setLayout(new BorderLayout(0, 12));
        setPreferredSize(new Dimension(LARGURA, ALTURA));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        add(montarCabecalho(), BorderLayout.NORTH);
        add(montarCentro(), BorderLayout.CENTER);
        add(montarBotoes(aoCopiar, aoEditar, aoAbrir, aoRemover), BorderLayout.SOUTH);

        // Clicar no cartao seleciona; duplo clique abre a edicao.
        MouseAdapter clique = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                aoSelecionar.run();
                if (e.getClickCount() == 2) aoEditar.run();
            }
        };
        propagarClique(this, clique);
    }

    public PasswordEntry getEntrada() {
        return entrada;
    }

    public void setSelecionada(boolean selecionada) {
        this.selecionada = selecionada;
        repaint();
    }

    public void setSenhaVisivel(boolean visivel) {
        this.senhaVisivel = visivel;
        if (labelSenha != null) {
            labelSenha.setText(visivel ? nulo(entrada.getSenha()) : mascarar(entrada.getSenha()));
            labelSenha.setForeground(visivel
                    ? UiUtil.cor("Label.foreground", new Color(0x212121))
                    : UiUtil.cor("Label.disabledForeground", new Color(0x9E9E9E)));
        }
        repaint();
    }

    // ------------------------------------------------------------
    // Montagem do conteudo
    // ------------------------------------------------------------

    private JComponent montarCabecalho() {
        JPanel topo = new JPanel(new BorderLayout(8, 0));
        topo.setOpaque(false);

        JPanel esquerda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        esquerda.setOpaque(false);
        if (mostrarIcone) {
            esquerda.add(new Badge(letra(), corBadge()));
        }
        JLabel nome = new JLabel(truncar(nulo(entrada.getSite()), 20));
        nome.setFont(nome.getFont().deriveFont(Font.BOLD, 13f));
        nome.setForeground(UiUtil.cor("Label.foreground", new Color(0x212121)));
        esquerda.add(nome);
        topo.add(esquerda, BorderLayout.WEST);

        topo.add(UiUtil.chip(entrada.getCategoria()), BorderLayout.EAST);
        return topo;
    }

    private JComponent montarCentro() {
        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));

        JLabel usuario = new JLabel(truncar(nulo(entrada.getUsuario()), 34));
        usuario.setFont(usuario.getFont().deriveFont(Font.PLAIN, 12f));
        usuario.setForeground(UiUtil.cor("Label.foreground", new Color(0x424242)));
        usuario.setAlignmentX(LEFT_ALIGNMENT);
        centro.add(usuario);

        String url = nulo(entrada.getUrl());
        if (!url.isBlank()) {
            JLabel link = new JLabel(truncar(url, 34));
            link.setFont(link.getFont().deriveFont(Font.PLAIN, 10f));
            link.setForeground(UiUtil.cor("Label.disabledForeground", new Color(0x9E9E9E)));
            link.setAlignmentX(LEFT_ALIGNMENT);
            centro.add(Box.createVerticalStrut(2));
            centro.add(link);
        }

        labelSenha = new JLabel(senhaVisivel ? nulo(entrada.getSenha()) : mascarar(entrada.getSenha()));
        labelSenha.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        labelSenha.setForeground(senhaVisivel
                ? UiUtil.cor("Label.foreground", new Color(0x212121))
                : UiUtil.cor("Label.disabledForeground", new Color(0x9E9E9E)));
        labelSenha.setAlignmentX(LEFT_ALIGNMENT);
        centro.add(Box.createVerticalStrut(4));
        centro.add(labelSenha);
        return centro;
    }

    private JComponent montarBotoes(Runnable aoCopiar, Runnable aoEditar,
                                    Runnable aoAbrir, Runnable aoRemover) {
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        botoes.setOpaque(false);

        JButton copiar = UiUtil.botaoPequenoPrimario("Copiar");
        copiar.addActionListener(e -> aoCopiar.run());
        botoes.add(copiar);

        JButton editar = UiUtil.botaoPequenoSecundario("Editar");
        editar.addActionListener(e -> aoEditar.run());
        botoes.add(editar);

        String url = nulo(entrada.getUrl());
        if (!url.isBlank()) {
            JButton abrir = UiUtil.botaoPequenoSecundario("Abrir");
            abrir.addActionListener(e -> aoAbrir.run());
            botoes.add(abrir);
        }

        JButton remover = UiUtil.botaoPequenoSecundario("Excluir");
        remover.addActionListener(e -> aoRemover.run());
        botoes.add(remover);
        return botoes;
    }

    // ------------------------------------------------------------
    // Pintura do cartao (fundo arredondado + borda de selecao)
    // ------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int raio = 18;
        Shape forma = new RoundRectangle2D.Float(0.5f, 0.5f,
                getWidth() - 1f, getHeight() - 1f, raio, raio);
        g2.setColor(UiUtil.cor("TextField.background", new Color(0xFAFAFA)));
        g2.fill(forma);
        g2.setColor(selecionada
                ? UiUtil.cor("Component.accentColor", new Color(0x1E88E5))
                : UiUtil.cor("Component.borderColor", new Color(0xD0D0D0)));
        g2.setStroke(new BasicStroke(selecionada ? 2f : 1f));
        g2.draw(forma);
        g2.dispose();
        super.paintComponent(g);
    }

    // ------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------

    private char letra() {
        String s = nulo(entrada.getSite());
        return s.isEmpty() ? '?' : Character.toUpperCase(s.charAt(0));
    }

    private Color corBadge() {
        String s = nulo(entrada.getSite());
        return CORES[Math.abs(s.hashCode()) % CORES.length];
    }

    /** Propaga o clique para todo o cartao, menos nos botoes interativos. */
    private static void propagarClique(Container c, MouseAdapter a) {
        c.addMouseListener(a);
        for (Component filho : c.getComponents()) {
            if (filho instanceof JButton || filho instanceof JTextField) continue;
            if (filho instanceof Container) propagarClique((Container) filho, a);
        }
    }

    private static String mascarar(String senha) {
        if (senha == null || senha.isEmpty()) return "";
        return "\u2022".repeat(Math.min(10, Math.max(6, senha.length())));
    }

    private static String truncar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
    }

    private static String nulo(String s) {
        return s == null ? "" : s;
    }

    /** Badge quadrado arredondado com a letra inicial do site. */
    private static final class Badge extends JComponent {
        private final char letra;
        private final Color cor;

        Badge(char letra, Color cor) {
            this.letra = letra;
            this.cor = cor;
            setPreferredSize(new Dimension(36, 36));
            setMinimumSize(new Dimension(36, 36));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(cor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 15f));
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.charWidth(letra)) / 2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(String.valueOf(letra), x, y);
            g2.dispose();
        }
    }
}
