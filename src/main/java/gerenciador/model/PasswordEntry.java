package gerenciador.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representa uma entrada individual de senha armazenada no cofre.
 * Alem dos campos basicos, guarda campos personalizados (nome/valor)
 * e um historico limitado de senhas anteriores.
 */
public class PasswordEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int MAX_HISTORICO = 5;

    private String id;
    private String site;
    private String usuario;
    private String senha;
    private String url;
    private String notas;
    private String categoria;
    private List<CampoPersonalizado> campos = new ArrayList<>();
    private List<String> historicoSenhas = new ArrayList<>();

    public PasswordEntry() {
        this.id = UUID.randomUUID().toString();
    }

    public PasswordEntry(String site, String usuario, String senha, String url, String notas) {
        this(site, usuario, senha, url, notas, "");
    }

    public PasswordEntry(String site, String usuario, String senha, String url, String notas, String categoria) {
        this.id = UUID.randomUUID().toString();
        this.site = site;
        this.usuario = usuario;
        this.senha = senha;
        this.url = url;
        this.notas = notas;
        this.categoria = categoria;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public List<CampoPersonalizado> getCampos() {
        if (campos == null) campos = new ArrayList<>();
        return campos;
    }
    public void setCampos(List<CampoPersonalizado> campos) { this.campos = campos; }

    public List<String> getHistoricoSenhas() {
        if (historicoSenhas == null) historicoSenhas = new ArrayList<>();
        return historicoSenhas;
    }
    public void setHistoricoSenhas(List<String> historicoSenhas) { this.historicoSenhas = historicoSenhas; }

    /**
     * Guarda a senha atual no historico (se for diferente da atual) e limita
     * o tamanho. Usado quando a senha da entrada e alterada.
     */
    public void arquivarSenhaAtual() {
        List<String> historico = getHistoricoSenhas();
        if (senha != null && !senha.isEmpty() && !historico.contains(senha)) {
            historico.add(0, senha);
            while (historico.size() > MAX_HISTORICO) {
                historico.remove(historico.size() - 1);
            }
        }
    }
}
