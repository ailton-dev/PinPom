package gerenciador.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Representa uma identidade (dados pessoais) guardada no cofre:
 * nome, CPF, nome social, endereco, telefone, e-mail e foto.
 */
public class Identidade implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String nome;
    private String cpf;
    private String nomeSocial;
    private String endereco;
    private String telefone;
    private String email;
    private byte[] foto;

    public Identidade() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getNomeSocial() { return nomeSocial; }
    public void setNomeSocial(String nomeSocial) { this.nomeSocial = nomeSocial; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public byte[] getFoto() { return foto; }
    public void setFoto(byte[] foto) { this.foto = foto; }
}
