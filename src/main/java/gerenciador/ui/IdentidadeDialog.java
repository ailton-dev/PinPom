package gerenciador.ui;

import gerenciador.model.Identidade;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * Dialogo para criar/editar/visualizar uma identidade, com foto opcional
 * (a foto e armazenada dentro do cofre cifrado).
 */
public class IdentidadeDialog extends JDialog {

    private JTextField campoNome;
    private JTextField campoCpf;
    private JTextField campoNomeSocial;
    private JTextField campoEndereco;
    private JTextField campoTelefone;
    private JTextField campoEmail;
    private JLabel labelFoto;
    private byte[] fotoBytes;

    private boolean confirmado = false;
    private final Identidade identidade;

    public IdentidadeDialog(Window owner, Identidade existente) {
        super(owner, existente == null ? "Nova identidade" : "Editar identidade", ModalityType.APPLICATION_MODAL);
        this.identidade = existente == null ? new Identidade() : existente;
        montarInterface();
        preencher();
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

        campoNome = new JTextField(24);
        addCampo(painel, gbc, linha++, "Nome:", campoNome);

        campoCpf = new JTextField(24);
        addCampo(painel, gbc, linha++, "CPF:", campoCpf);

        campoNomeSocial = new JTextField(24);
        addCampo(painel, gbc, linha++, "Nome social:", campoNomeSocial);

        campoEndereco = new JTextField(24);
        addCampo(painel, gbc, linha++, "Endereco:", campoEndereco);

        campoTelefone = new JTextField(24);
        addCampo(painel, gbc, linha++, "Telefone:", campoTelefone);

        campoEmail = new JTextField(24);
        addCampo(painel, gbc, linha++, "E-mail:", campoEmail);

        // ---- Foto ----
        gbc.gridx = 0; gbc.gridy = linha;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        painel.add(new JLabel("Foto:"), gbc);

        JPanel painelFoto = new JPanel(new BorderLayout(8, 0));
        labelFoto = new JLabel();
        labelFoto.setPreferredSize(new Dimension(120, 120));
        labelFoto.setHorizontalAlignment(SwingConstants.CENTER);
        labelFoto.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        labelFoto.setText("Sem foto");
        painelFoto.add(labelFoto, BorderLayout.CENTER);

        JPanel painelBotoesFoto = new JPanel();
        painelBotoesFoto.setLayout(new BoxLayout(painelBotoesFoto, BoxLayout.Y_AXIS));
        JButton escolherFoto = new JButton("Escolher foto...");
        escolherFoto.addActionListener(e -> escolherFoto());
        JButton removerFoto = new JButton("Remover foto");
        removerFoto.addActionListener(e -> {
            fotoBytes = null;
            labelFoto.setIcon(null);
            labelFoto.setText("Sem foto");
        });
        painelBotoesFoto.add(escolherFoto);
        painelBotoesFoto.add(Box.createVerticalStrut(4));
        painelBotoesFoto.add(removerFoto);
        painelFoto.add(painelBotoesFoto, BorderLayout.EAST);

        gbc.gridx = 1; gbc.gridy = linha++;
        painel.add(painelFoto, gbc);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> {
            confirmado = false;
            dispose();
        });
        JButton salvar = new JButton("Salvar");
        salvar.addActionListener(e -> salvar());
        painelBotoes.add(cancelar);
        painelBotoes.add(salvar);

        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 2;
        painel.add(painelBotoes, gbc);

        getRootPane().setDefaultButton(salvar);
        setContentPane(painel);
    }

    private void addCampo(JPanel painel, GridBagConstraints gbc, int linha, String rotulo, JComponent campo) {
        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        painel.add(new JLabel(rotulo), gbc);
        gbc.gridx = 1;
        painel.add(campo, gbc);
    }

    private void preencher() {
        campoNome.setText(nulo(identidade.getNome()));
        campoCpf.setText(nulo(identidade.getCpf()));
        campoNomeSocial.setText(nulo(identidade.getNomeSocial()));
        campoEndereco.setText(nulo(identidade.getEndereco()));
        campoTelefone.setText(nulo(identidade.getTelefone()));
        campoEmail.setText(nulo(identidade.getEmail()));
        if (identidade.getFoto() != null) {
            setFoto(identidade.getFoto());
        }
    }

    private void escolherFoto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecionar foto");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Imagens (JPG, PNG, GIF, BMP)", "jpg", "jpeg", "png", "gif", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            File arquivo = chooser.getSelectedFile();
            BufferedImage img = ImageIO.read(arquivo);
            if (img == null) {
                JOptionPane.showMessageDialog(this, "Arquivo de imagem invalido.",
                        "Foto", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Redimensiona para no maximo 512x512 e converte para PNG.
            BufferedImage redimensionada = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = redimensionada.createGraphics();
            double escala = Math.min(512.0 / img.getWidth(), 512.0 / img.getHeight());
            int w = (int) (img.getWidth() * escala);
            int h = (int) (img.getHeight() * escala);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, (512 - w) / 2, (512 - h) / 2, w, h, null);
            g.dispose();

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            ImageIO.write(redimensionada, "png", out);
            setFoto(out.toByteArray());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar foto: " + ex.getMessage(),
                    "Foto", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setFoto(byte[] bytes) {
        try {
            this.fotoBytes = bytes;
            ImageIcon icone = new ImageIcon(ImageIO.read(new ByteArrayInputStream(bytes)));
            labelFoto.setIcon(new ImageIcon(icone.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH)));
            labelFoto.setText("");
        } catch (Exception ex) {
            this.fotoBytes = null;
            labelFoto.setText("Foto invalida");
        }
    }

    private void salvar() {
        if (campoNome.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe o nome.",
                    "Campo obrigatorio", JOptionPane.WARNING_MESSAGE);
            return;
        }
        identidade.setNome(campoNome.getText().trim());
        identidade.setCpf(campoCpf.getText().trim());
        identidade.setNomeSocial(campoNomeSocial.getText().trim());
        identidade.setEndereco(campoEndereco.getText().trim());
        identidade.setTelefone(campoTelefone.getText().trim());
        identidade.setEmail(campoEmail.getText().trim());
        identidade.setFoto(fotoBytes);
        confirmado = true;
        dispose();
    }

    private static String nulo(String s) {
        return s == null ? "" : s;
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public Identidade getIdentidade() {
        return identidade;
    }
}
