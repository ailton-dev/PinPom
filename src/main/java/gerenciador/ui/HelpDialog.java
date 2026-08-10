package gerenciador.ui;

import gerenciador.util.AppInfo;

import javax.swing.*;
import java.awt.*;

/**
 * Painel de ajuda: guia rapido de uso, atalhos de teclado e notas de seguranca.
 */
public class HelpDialog extends JDialog {

    public HelpDialog(Window owner) {
        super(owner, "Ajuda", ModalityType.APPLICATION_MODAL);

        String conteudo = """
            <html><div style='width:470px'>
            <h3>Como usar</h3>
            <ul>
              <li>Na primeira vez, crie uma <b>senha mestre</b>. Ela protege todo o cofre.</li>
              <li>Na aba <b>Senhas</b>, use <b>Adicionar</b> para guardar uma nova senha e
                  <b>Editar</b> para corrigir. Cada entrada pode ter <b>campos personalizados</b>
                  (ex.: chave de API, host SSH) e mantem <b>historico de senhas anteriores</b>.</li>
              <li><b>Gerar senha forte</b> cria senhas aleatorias com tamanho e caracteres a escolher.</li>
              <li>Na aba <b>Identidades</b>, guarde nome, CPF, endereco, telefone, e-mail e foto
                  (tudo fica cifrado dentro do cofre).</li>
              <li><b>Copiar senha</b> coloca a senha na area de transferencia (com opcao de
                  limpeza automatica em Configuracoes).</li>
              <li>Use <b>Verificar senhas</b> para encontrar senhas reutilizadas ou fracas.
                  Linhas alaranjadas indicam reuso; vermelhas, senha fraca.</li>
              <li>Faca <b>Backup</b> periodico do cofre e guarde o arquivo em local seguro.</li>
              <li>O cofre e salvo automaticamente a cada alteracao.</li>
            </ul>

            <h3>Atalhos de teclado</h3>
            <ul>
              <li><b>Ctrl+N</b> - nova entrada</li>
              <li><b>F2</b> - editar entrada selecionada</li>
              <li><b>Delete</b> - remover entrada selecionada</li>
              <li><b>Ctrl+C</b> - copiar senha da entrada selecionada</li>
              <li><b>Ctrl+D</b> - duplicar entrada selecionada</li>
              <li><b>Ctrl+F</b> - focar na busca</li>
              <li><b>Ctrl+</b> - configuracoes</li>
              <li><b>Ctrl+T</b> - alternar tema claro/escuro</li>
              <li><b>Ctrl+L</b> - bloquear (voltar para a senha mestre / PIN)</li>
              <li><b>F1</b> - abrir esta ajuda</li>
            </ul>

            <h3>Seguranca</h3>
            <ul>
              <li>As senhas ficam cifradas com <b>AES-256-GCM</b>; a chave e derivada da
                  senha mestre com PBKDF2 (150.000 iteracoes).</li>
              <li>A senha mestre nunca e gravada em disco. O <b>PIN</b> de desbloqueio guarda
                  apenas a chave do cofre embrulhada (nunca a senha mestre em claro).</li>
              <li>Nao existe recuperacao: se voce esquecer a senha mestre, os dados
                  nao podem ser recuperados por ninguem.</li>
              <li>Quando a opcao <b>Permitir captura de tela</b> esta desmarcada, as senhas sao
                  ocultadas automaticamente quando a janela perde o foco.</li>
              <li>O arquivo do cofre fica em <code>~/.gerenciador-senhas/cofre.dat</code>.</li>
            </ul>

            <h3>Sobre o projeto</h3>
            <p>Projeto local, de codigo aberto e sem conexao com a internet. Os dados
               ficam apenas no seu computador. Veja o painel <b>Ajuda &gt; Sobre</b>
               para conhecer o criador.</p>
            </div></html>
            """;

        JEditorPane texto = new JEditorPane();
        texto.setContentType("text/html");
        texto.setText(conteudo);
        texto.setEditable(false);
        texto.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        texto.setBackground(getBackground());

        JButton botaoFechar = new JButton("Fechar");
        botaoFechar.addActionListener(e -> dispose());

        JPanel painel = new JPanel(new BorderLayout());
        painel.add(new JScrollPane(texto), BorderLayout.CENTER);
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.add(botaoFechar);
        painel.add(rodape, BorderLayout.SOUTH);

        setContentPane(painel);
        getRootPane().setDefaultButton(botaoFechar);
        setSize(540, 560);
        setLocationRelativeTo(owner);
    }
}
