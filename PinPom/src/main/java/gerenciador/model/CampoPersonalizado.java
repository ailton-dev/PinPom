package gerenciador.model;

/**
 * Um campo personalizado (nome/valor) de uma entrada de senha.
 * Ex.: "Chave de API", "Pergunta de seguranca", "Host SSH", "Porta".
 */
public class CampoPersonalizado {
    private String nome;
    private String valor;

    public CampoPersonalizado() {
    }

    public CampoPersonalizado(String nome, String valor) {
        this.nome = nome;
        this.valor = valor;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
}
