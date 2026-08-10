package gerenciador.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gerenciador.crypto.CryptoUtil;
import gerenciador.crypto.CryptoUtil.SenhaIncorretaException;
import gerenciador.model.PasswordEntry;
import gerenciador.model.VaultData;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Responsavel por ler/gravar o arquivo do cofre (criptografado em disco),
 * por importar/exportar entradas em CSV e por fazer backup/restauracao.
 *
 * Local padrao do cofre: ~/.gerenciador-senhas/cofre.dat
 *
 * O conteudo e serializado como JSON (via Gson) e cifrado com AES-256-GCM.
 * Cofres criados por versoes antigas (formato texto/base64) sao migrados
 * automaticamente para o novo formato na primeira abertura.
 */
public class VaultManager {

    private final Path arquivoCofre;
    private SecretKey chaveAtual;
    private byte[] saltAtual;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public VaultManager() {
        this(Paths.get(System.getProperty("user.home"), ".gerenciador-senhas"));
    }

    /** Cria um gerenciador que usa o diretorio informado (usado em testes). */
    public VaultManager(Path diretorio) {
        Path dir = diretorio;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Nao foi possivel criar diretorio de dados: " + dir, e);
        }
        this.arquivoCofre = dir.resolve("cofre.dat");
    }

    public boolean cofreExiste() {
        return Files.exists(arquivoCofre);
    }

    /**
     * Cria um novo cofre vazio protegido pela senha mestre informada.
     */
    public void criarCofre(char[] senhaMestre) throws IOException {
        saltAtual = CryptoUtil.randomBytes(CryptoUtil.SALT_LEN);
        chaveAtual = CryptoUtil.deriveKey(senhaMestre, saltAtual);
        salvar(new VaultData());
    }

    /**
     * Abre o cofre existente com a senha mestre informada.
     * Lanca SenhaIncorretaException se a senha estiver errada.
     */
    public VaultData abrirCofre(char[] senhaMestre) throws IOException, SenhaIncorretaException {
        byte[] conteudo = Files.readAllBytes(arquivoCofre);
        if (conteudo.length < CryptoUtil.SALT_LEN) {
            throw new SenhaIncorretaException("Arquivo do cofre invalido ou corrompido");
        }
        byte[] salt = Arrays.copyOfRange(conteudo, 0, CryptoUtil.SALT_LEN);
        byte[] blobCifrado = Arrays.copyOfRange(conteudo, CryptoUtil.SALT_LEN, conteudo.length);

        SecretKey chave = CryptoUtil.deriveKey(senhaMestre, salt);
        byte[] plano = CryptoUtil.decrypt(blobCifrado, chave); // lanca excecao se senha errada

        this.saltAtual = salt;
        this.chaveAtual = chave;
        return desserializar(plano);
    }

    /**
     * Abre o cofre usando uma chave ja recuperada (ex.: desbloqueio por PIN).
     * A chave em bytes e verificada contra o cofre na abertura.
     */
    public VaultData abrirCofreComChave(byte[] salt, byte[] chaveBytes) throws IOException, SenhaIncorretaException {
        byte[] conteudo = Files.readAllBytes(arquivoCofre);
        if (conteudo.length < CryptoUtil.SALT_LEN) {
            throw new SenhaIncorretaException("Arquivo do cofre invalido ou corrompido");
        }
        byte[] blobCifrado = Arrays.copyOfRange(conteudo, CryptoUtil.SALT_LEN, conteudo.length);

        SecretKey chave = new SecretKeySpec(chaveBytes, "AES");
        byte[] plano = CryptoUtil.decrypt(blobCifrado, chave); // falha se a chave nao for a certa

        this.saltAtual = salt;
        this.chaveAtual = chave;
        return desserializar(plano);
    }

    /**
     * Abre o cofre usando um PIN configurado (a chave embrulhada pelo PIN e
     * recuperada do config.properties e validada contra o cofre).
     */
    public VaultData abrirCofreComPin(String pin) throws IOException, SenhaIncorretaException {
        byte[] conteudo = Files.readAllBytes(arquivoCofre);
        if (conteudo.length < CryptoUtil.SALT_LEN) {
            throw new SenhaIncorretaException("Arquivo do cofre invalido ou corrompido");
        }
        byte[] salt = Arrays.copyOfRange(conteudo, 0, CryptoUtil.SALT_LEN);
        try {
            byte[] chaveBytes = gerenciador.util.ConfigStore.desbloquearComPin(pin);
            return abrirCofreComChave(salt, chaveBytes);
        } catch (SenhaIncorretaException e) {
            throw e;
        } catch (Exception e) {
            throw new SenhaIncorretaException("PIN incorreto");
        }
    }

    /**
     * Salva os dados do cofre no arquivo, cifrando com a chave atualmente
     * carregada (definida por criarCofre/abrirCofre).
     */
    public void salvar(VaultData dados) throws IOException {
        if (chaveAtual == null || saltAtual == null) {
            throw new IllegalStateException("Cofre nao esta aberto/inicializado");
        }
        byte[] plano = serializar(dados);
        byte[] blobCifrado = CryptoUtil.encrypt(plano, chaveAtual);

        byte[] saida = new byte[saltAtual.length + blobCifrado.length];
        System.arraycopy(saltAtual, 0, saida, 0, saltAtual.length);
        System.arraycopy(blobCifrado, 0, saida, saltAtual.length, blobCifrado.length);

        // Grava em arquivo temporario e depois move, para evitar corromper
        // o cofre em caso de falha no meio da escrita.
        Path tmp = Files.createTempFile(arquivoCofre.getParent(), "cofre", ".tmp");
        Files.write(tmp, saida);
        Files.move(tmp, arquivoCofre, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Troca a senha mestre: deriva um novo salt/chave e regrava o cofre.
     */
    public void alterarSenhaMestre(char[] novaSenha, VaultData dados) throws IOException {
        byte[] novoSalt = CryptoUtil.randomBytes(CryptoUtil.SALT_LEN);
        SecretKey novaChave = CryptoUtil.deriveKey(novaSenha, novoSalt);
        this.saltAtual = novoSalt;
        this.chaveAtual = novaChave;
        salvar(dados);
    }

    // ---------------------------------------------------------------
    // Backup / restauracao
    // ---------------------------------------------------------------

    /**
     * Copia o arquivo cifrado do cofre para o destino escolhido (backup).
     */
    public void fazerBackup(File destino) throws IOException {
        if (!cofreExiste()) {
            throw new IOException("Cofre nao existe para fazer backup");
        }
        Files.copy(arquivoCofre, destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Restaura um backup: verifica se o arquivo abre com a chave atual e,
     * em caso positivo, substitui o cofre atual e retorna os dados.
     */
    public VaultData restaurarBackup(File origem) throws IOException, SenhaIncorretaException {
        if (chaveAtual == null) {
            throw new IllegalStateException("Cofre nao esta aberto/inicializado");
        }
        byte[] conteudo = Files.readAllBytes(origem.toPath());
        if (conteudo.length < CryptoUtil.SALT_LEN) {
            throw new SenhaIncorretaException("Arquivo de backup invalido ou corrompido");
        }
        byte[] salt = Arrays.copyOfRange(conteudo, 0, CryptoUtil.SALT_LEN);
        byte[] blobCifrado = Arrays.copyOfRange(conteudo, CryptoUtil.SALT_LEN, conteudo.length);
        byte[] plano = CryptoUtil.decrypt(blobCifrado, chaveAtual); // valida com a chave atual

        // Depois de validado, copia por cima do cofre atual e atualiza a chave
        // carregada caso o salt do backup seja diferente.
        this.saltAtual = salt;
        salvar(desserializar(plano));
        return desserializar(plano);
    }

    // ---------------------------------------------------------------
    // Serializacao interna (JSON com Gson; migra o formato antigo)
    // ---------------------------------------------------------------

    private byte[] serializar(VaultData dados) {
        return gson.toJson(dados).getBytes(StandardCharsets.UTF_8);
    }

    private VaultData desserializar(byte[] dados) {
        String texto = new String(dados, StandardCharsets.UTF_8).trim();
        if (texto.isEmpty()) return new VaultData();

        if (texto.startsWith("{")) {
            VaultData dadosJson = gson.fromJson(texto, VaultData.class);
            if (dadosJson == null) return new VaultData();
            if (dadosJson.getEntradas() == null) dadosJson.setEntradas(new ArrayList<>());
            if (dadosJson.getIdentidades() == null) dadosJson.setIdentidades(new ArrayList<>());
            return dadosJson;
        }

        // Formato legado: linhas CSV de campos em Base64.
        VaultData legado = new VaultData();
        legado.setEntradas(desserializarLegado(texto));
        return legado;
    }

    private List<PasswordEntry> desserializarLegado(String texto) {
        List<PasswordEntry> lista = new ArrayList<>();
        if (texto.isEmpty()) return lista;
        for (String linha : texto.split("\n", -1)) {
            if (linha.isBlank()) continue;
            String[] campos = linha.split(",", -1);
            if (campos.length < 6) continue;
            PasswordEntry e = new PasswordEntry();
            e.setId(unb64(campos[0]));
            e.setSite(unb64(campos[1]));
            e.setUsuario(unb64(campos[2]));
            e.setSenha(unb64(campos[3]));
            e.setUrl(unb64(campos[4]));
            e.setNotas(unb64(campos[5]));
            // Campo categoria existe apenas em cofres de versoes intermediarias;
            // cofres antigos (6 campos) ficam com categoria vazia.
            e.setCategoria(campos.length > 6 ? unb64(campos[6]) : "");
            lista.add(e);
        }
        return lista;
    }

    private static String b64(String s) {
        if (s == null) s = "";
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String unb64(String s) {
        if (s == null || s.isEmpty()) return "";
        return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------
    // Importacao / exportacao CSV
    // ---------------------------------------------------------------

    /**
     * Cabecalho esperado/gerado no CSV: site,usuario,senha,url,notas,categoria
     * A leitura tambem aceita cabecalhos comuns exportados por navegadores
     * (name/url/username/password) e tenta mapear as colunas.
     */
    public List<PasswordEntry> importarCsv(File arquivo) throws IOException {
        String conteudo = Files.readString(arquivo.toPath(), StandardCharsets.UTF_8);
        List<List<String>> registros = parseCsv(conteudo);
        List<PasswordEntry> resultado = new ArrayList<>();
        if (registros.isEmpty()) return resultado;

        List<String> cabecalho = registros.get(0);
        Map<String, Integer> indice = new HashMap<>();
        for (int i = 0; i < cabecalho.size(); i++) {
            indice.put(cabecalho.get(i).trim().toLowerCase(Locale.ROOT), i);
        }

        int idxSite = primeiroIndice(indice, "site", "name", "title", "nome");
        int idxUser = primeiroIndice(indice, "usuario", "username", "user", "login");
        int idxSenha = primeiroIndice(indice, "senha", "password", "pass");
        int idxUrl = primeiroIndice(indice, "url", "link", "website");
        int idxNotas = primeiroIndice(indice, "notas", "note", "notes", "obs", "observacoes");
        int idxCategoria = primeiroIndice(indice, "categoria", "category", "grupo", "folder");

        // Se o cabecalho nao bateu com nada conhecido, assume que a primeira
        // linha ja e dado (nao ha cabecalho) e usa ordem padrao.
        boolean semCabecalhoReconhecido = (idxSite < 0 && idxUser < 0 && idxSenha < 0 && idxUrl < 0);

        for (int i = semCabecalhoReconhecido ? 0 : 1; i < registros.size(); i++) {
            List<String> campos = registros.get(i);
            if (campos.size() == 1 && campos.get(0).isBlank()) continue;
            PasswordEntry e = new PasswordEntry();
            if (semCabecalhoReconhecido) {
                e.setSite(campoOu(campos, 0, ""));
                e.setUsuario(campoOu(campos, 1, ""));
                e.setSenha(campoOu(campos, 2, ""));
                e.setUrl(campoOu(campos, 3, ""));
                e.setNotas(campoOu(campos, 4, ""));
                e.setCategoria("");
            } else {
                e.setSite(campoOu(campos, idxSite, ""));
                e.setUsuario(campoOu(campos, idxUser, ""));
                e.setSenha(campoOu(campos, idxSenha, ""));
                e.setUrl(campoOu(campos, idxUrl, ""));
                e.setNotas(campoOu(campos, idxNotas, ""));
                e.setCategoria(campoOu(campos, idxCategoria, ""));
            }
            resultado.add(e);
        }
        return resultado;
    }

    public void exportarCsv(File arquivo, List<PasswordEntry> entradas) throws IOException {
        try (java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(arquivo), StandardCharsets.UTF_8))) {
            bw.write("site,usuario,senha,url,notas,categoria");
            bw.newLine();
            for (PasswordEntry e : entradas) {
                bw.write(csvCampo(e.getSite()) + ',' +
                        csvCampo(e.getUsuario()) + ',' +
                        csvCampo(e.getSenha()) + ',' +
                        csvCampo(e.getUrl()) + ',' +
                        csvCampo(e.getNotas()) + ',' +
                        csvCampo(e.getCategoria()));
                bw.newLine();
            }
        }
    }

    private static String csvCampo(String valor) {
        if (valor == null) valor = "";
        boolean precisaAspas = valor.contains(",") || valor.contains("\"") || valor.contains("\n");
        String v = valor.replace("\"", "\"\"");
        return precisaAspas ? "\"" + v + "\"" : v;
    }

    /**
     * Parser de CSV que respeita aspas, virgulas e quebras de linha escapadas
     * dentro de campos entre aspas. Retorna a lista de registros (linhas).
     */
    private static List<List<String>> parseCsv(String texto) {
        List<List<String>> registros = new ArrayList<>();
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean dentroDeAspas = false;
        int n = texto.length();
        for (int i = 0; i < n; i++) {
            char c = texto.charAt(i);
            if (dentroDeAspas) {
                if (c == '"') {
                    if (i + 1 < n && texto.charAt(i + 1) == '"') {
                        atual.append('"');
                        i++;
                    } else {
                        dentroDeAspas = false;
                    }
                } else {
                    atual.append(c);
                }
            } else {
                if (c == '"') {
                    dentroDeAspas = true;
                } else if (c == ',') {
                    campos.add(atual.toString());
                    atual.setLength(0);
                } else if (c == '\n') {
                    campos.add(atual.toString());
                    atual.setLength(0);
                    registros.add(campos);
                    campos = new ArrayList<>();
                } else if (c != '\r') {
                    atual.append(c);
                }
            }
        }
        campos.add(atual.toString());
        registros.add(campos);
        return registros;
    }

    private static int primeiroIndice(Map<String, Integer> indice, String... chaves) {
        for (String k : chaves) {
            Integer v = indice.get(k);
            if (v != null) return v;
        }
        return -1;
    }

    private static String campoOu(List<String> campos, int idx, String padrao) {
        if (idx < 0 || idx >= campos.size()) return padrao;
        String v = campos.get(idx);
        return v == null ? padrao : v;
    }

    public Path getArquivoCofre() {
        return arquivoCofre;
    }

    public byte[] getSaltAtual() {
        return saltAtual;
    }

    public SecretKey getChaveAtual() {
        return chaveAtual;
    }
}
