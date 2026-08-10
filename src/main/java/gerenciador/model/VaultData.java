package gerenciador.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Conteudo completo do cofre: lista de entradas de senha e lista de
 * identidades. Este objeto e serializado em JSON e cifrado em disco.
 */
public class VaultData {

    public static final int FORMATO_ATUAL = 2;

    private int formato = FORMATO_ATUAL;
    private List<PasswordEntry> entradas = new ArrayList<>();
    private List<Identidade> identidades = new ArrayList<>();

    public int getFormato() { return formato; }
    public void setFormato(int formato) { this.formato = formato; }

    public List<PasswordEntry> getEntradas() { return entradas; }
    public void setEntradas(List<PasswordEntry> entradas) { this.entradas = entradas; }

    public List<Identidade> getIdentidades() { return identidades; }
    public void setIdentidades(List<Identidade> identidades) { this.identidades = identidades; }
}
