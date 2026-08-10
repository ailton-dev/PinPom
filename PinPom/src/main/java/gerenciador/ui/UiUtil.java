package gerenciador.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Componentes estilizados do PinPom: botoes arredondados (primario/secundario,
 * tamanho normal ou compacto) e etiquetas "chip" de categoria. As cores sao
 * lidas do tema FlatLaf corrente (claro ou escuro), entao o visual acompanha
 * a troca de tema automaticamente.
 */
public final class UiUtil {

    private UiUtil() {
    }

    public static JButton botaoPrimario(String texto) {
        return novoBotao(texto, true, false);
    }

    public static JButton botaoSecundario(String texto) {
        return novoBotao(texto, false, false);
    }

    public static JButton botaoPequenoPrimario(String texto) {
        return novoBotao(texto, true, true);
    }

    public static JButton botaoPequenoSecundario(String texto) {
        return novoBotao(texto, false, true);
    }

    /** Chip arredondado usado para exibir a categoria (cor de destaque suave). */
    public static JLabel chip(String texto) {
        Color accent = cor("Component.accentColor", new Color(0x1E88E5));
        Color fundo = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 45);
        JLabel l = new JLabel(texto == null || texto.isBlank() ? "Geral" : texto) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(fundo);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                super.paintComponent(g);
            }
        };
        l.setOpaque(false);
        l.setBorder(new EmptyBorder(3, 10, 3, 10));
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11f));
        l.setForeground(accent);
        return l;
    }

    public static Component espaco(int largura) {
        return Box.createHorizontalStrut(largura);
    }

    /** Le uma cor do tema atual, com padrao caso o tema nao a defina. */
    public static Color cor(String chave, Color padrao) {
        Color c = UIManager.getColor(chave);
        return c == null ? padrao : c;
    }

    private static JButton novoBotao(String texto, boolean primario, boolean pequeno) {
        return new BotaoArredondado(texto, primario, pequeno);
    }

    /** Botao com fundo arredondado e efeito de hover/press. */
    private static final class BotaoArredondado extends JButton {
        private final boolean primario;
        private boolean hover;

        BotaoArredondado(String texto, boolean primario, boolean pequeno) {
            super(texto);
            this.primario = primario;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setRolloverEnabled(true);
            if (pequeno) {
                setBorder(new EmptyBorder(5, 10, 5, 10));
                setFont(getFont().deriveFont(Font.PLAIN, 11f));
            } else {
                setBorder(new EmptyBorder(8, 16, 8, 16));
                setFont(getFont().deriveFont(Font.BOLD, 12f));
            }
            setForeground(primario ? Color.WHITE
                    : cor("Button.foreground", new Color(0x303030)));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int raio = Math.min(20, getHeight());
            Color base;
            if (primario) {
                base = cor("Component.accentColor", new Color(0x1E88E5));
                if (getModel().isPressed()) base = base.darker();
                else if (hover) base = base.brighter();
            } else {
                base = cor("Button.background", new Color(0xE8E8E8));
                if (getModel().isPressed()) base = base.darker();
                else if (hover) base = cor("Button.hoverBackground", base.brighter());
            }
            g2.setColor(base);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, raio, raio);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
