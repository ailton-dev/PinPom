package gerenciador.storage;

import gerenciador.crypto.CryptoUtil;
import gerenciador.crypto.CryptoUtil.SenhaIncorretaException;
import gerenciador.model.CampoPersonalizado;
import gerenciador.model.Identidade;
import gerenciador.model.PasswordEntry;
import gerenciador.model.VaultData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VaultManagerTest {

    @TempDir
    Path dir;

    private static VaultData vaultCom(List<PasswordEntry> entradas) {
        VaultData vd = new VaultData();
        vd.setEntradas(new ArrayList<>(entradas));
        return vd;
    }

    @Test
    void cofreRedondoCriarSalvarAbrir() throws Exception {
        VaultManager vm = new VaultManager(dir);
        vm.criarCofre("senhaMestre".toCharArray());

        PasswordEntry e = new PasswordEntry("GitHub", "usuario", "senha123",
                "https://github.com", "pessoal", "Trabalho");
        e.getCampos().add(new CampoPersonalizado("Token", "abc123"));
        vm.salvar(vaultCom(List.of(e)));

        VaultManager aberto = new VaultManager(dir);
        List<PasswordEntry> lidas = aberto.abrirCofre("senhaMestre".toCharArray()).getEntradas();
        assertEquals(1, lidas.size());
        PasswordEntry lida = lidas.get(0);
        assertEquals("GitHub", lida.getSite());
        assertEquals("usuario", lida.getUsuario());
        assertEquals("senha123", lida.getSenha());
        assertEquals("https://github.com", lida.getUrl());
        assertEquals("pessoal", lida.getNotas());
        assertEquals("Trabalho", lida.getCategoria());
        assertEquals(1, lida.getCampos().size());
        assertEquals("abc123", lida.getCampos().get(0).getValor());
    }

    @Test
    void identidadesSaoPersistidasComFoto() throws Exception {
        VaultManager vm = new VaultManager(dir);
        vm.criarCofre("senha".toCharArray());

        Identidade id = new Identidade();
        id.setNome("Maria");
        id.setCpf("123.456.789-00");
        id.setNomeSocial("Mari");
        id.setEndereco("Rua A, 1");
        id.setTelefone("(11) 99999-0000");
        id.setEmail("maria@exemplo.com");
        id.setFoto(new byte[]{1, 2, 3, 4, 5});
        VaultData vd = new VaultData();
        vd.getIdentidades().add(id);
        vm.salvar(vd);

        VaultManager aberto = new VaultManager(dir);
        VaultData lido = aberto.abrirCofre("senha".toCharArray());
        assertEquals(1, lido.getIdentidades().size());
        Identidade lida = lido.getIdentidades().get(0);
        assertEquals("Maria", lida.getNome());
        assertEquals("Mari", lida.getNomeSocial());
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, lida.getFoto());
    }

    @Test
    void senhaMestreErradaFalhaAoAbrir() throws Exception {
        VaultManager vm = new VaultManager(dir);
        vm.criarCofre("correta".toCharArray());
        vm.salvar(vaultCom(List.of(new PasswordEntry("Site", "u", "p", "", ""))));

        VaultManager aberto = new VaultManager(dir);
        assertThrows(SenhaIncorretaException.class, () -> aberto.abrirCofre("errada".toCharArray()));
    }

    @Test
    void alterarSenhaMestreMantemDados() throws Exception {
        VaultManager vm = new VaultManager(dir);
        vm.criarCofre("antiga".toCharArray());
        vm.salvar(vaultCom(List.of(new PasswordEntry("Site", "u", "p", "", ""))));
        vm.alterarSenhaMestre("novaSenha".toCharArray(),
                vaultCom(List.of(new PasswordEntry("Site", "u", "p", "", ""))));

        VaultManager aberto = new VaultManager(dir);
        assertThrows(SenhaIncorretaException.class, () -> aberto.abrirCofre("antiga".toCharArray()));
        assertEquals(1, aberto.abrirCofre("novaSenha".toCharArray()).getEntradas().size());
    }

    @Test
    void cofreVazioAbreComListaVazia() throws Exception {
        VaultManager vm = new VaultManager(dir);
        vm.criarCofre("senha".toCharArray());
        vm.salvar(new VaultData());

        VaultManager aberto = new VaultManager(dir);
        assertTrue(aberto.abrirCofre("senha".toCharArray()).getEntradas().isEmpty());
    }

    @Test
    void formatoLegadoSemCategoriaEMigradoParaJson() throws Exception {
        VaultManager vm = new VaultManager(dir);
        vm.criarCofre("senha".toCharArray());
        // Grava um blob cifrado no formato legado (6 campos base64 por linha).
        Path arquivo = dir.resolve("cofre.dat");
        String linhaLegada = String.join(",",
                b64("id-1"), b64("Site"), b64("u"), b64("p"), b64(""), b64("")) + "\n";
        byte[] salt = CryptoUtil.randomBytes(CryptoUtil.SALT_LEN);
        javax.crypto.SecretKey chave = CryptoUtil.deriveKey("senha".toCharArray(), salt);
        byte[] blob = CryptoUtil.encrypt(linhaLegada.getBytes(StandardCharsets.UTF_8), chave);
        byte[] saida = new byte[salt.length + blob.length];
        System.arraycopy(salt, 0, saida, 0, salt.length);
        System.arraycopy(blob, 0, saida, salt.length, blob.length);
        Files.write(arquivo, saida);

        VaultManager aberto = new VaultManager(dir);
        VaultData lido = aberto.abrirCofre("senha".toCharArray());
        assertEquals(1, lido.getEntradas().size());
        assertEquals("Site", lido.getEntradas().get(0).getSite());
        assertEquals("", lido.getEntradas().get(0).getCategoria());

        // Ao salvar de novo, ja grava no novo formato JSON.
        aberto.salvar(lido);
        byte[] conteudo = Files.readAllBytes(arquivo);
        byte[] plano = CryptoUtil.decrypt(
                Arrays.copyOfRange(conteudo, 16, conteudo.length), chave);
        String texto = new String(plano, StandardCharsets.UTF_8);
        assertTrue(texto.trim().startsWith("{"), "deve migrar para JSON");
    }

    @Test
    void backupERestauracaoRedondos() throws Exception {
        VaultManager vm = new VaultManager(dir);
        vm.criarCofre("senha".toCharArray());
        vm.salvar(vaultCom(List.of(new PasswordEntry("GitHub", "u", "p", "", ""))));

        File backup = new File(dir.toFile(), "backup.dat");
        vm.fazerBackup(backup);
        assertTrue(backup.exists());

        // Simula perda de dados: salva outra coisa por cima.
        vm.salvar(vaultCom(List.of(new PasswordEntry("Outro", "x", "y", "", ""))));

        VaultData restaurado = vm.restaurarBackup(backup);
        assertEquals(1, restaurado.getEntradas().size());
        assertEquals("GitHub", restaurado.getEntradas().get(0).getSite());
    }

    @Test
    void restaurarBackupInvalidoFalha() throws Exception {
        VaultManager vm = new VaultManager(dir);
        vm.criarCofre("senha".toCharArray());
        vm.salvar(vaultCom(List.of()));

        File invalido = new File(dir.toFile(), "invalido.dat");
        Files.writeString(invalido.toPath(), "isto nao e um cofre cifrado");

        assertThrows(SenhaIncorretaException.class, () -> vm.restaurarBackup(invalido));
    }

    @Test
    void importarCsvComCabecalhoMapeiaColunas() throws Exception {
        VaultManager vm = new VaultManager(dir);
        File csv = new File(dir.toFile(), "import.csv");
        Files.writeString(csv.toPath(), """
                site,usuario,senha,url,notas,categoria
                GitHub,meu_usuario,MinhaSenha123!,https://github.com,conta pessoal,Trabalho
                """);

        List<PasswordEntry> importadas = vm.importarCsv(csv);
        assertEquals(1, importadas.size());
        assertEquals("GitHub", importadas.get(0).getSite());
        assertEquals("conta pessoal", importadas.get(0).getNotas());
        assertEquals("Trabalho", importadas.get(0).getCategoria());
    }

    @Test
    void importarCsvSemCabecalhoUsaOrdemPadrao() throws Exception {
        VaultManager vm = new VaultManager(dir);
        File csv = new File(dir.toFile(), "import.csv");
        Files.writeString(csv.toPath(), "Gmail,meu_email,Senha456#,https://gmail.com,nota\n");

        List<PasswordEntry> importadas = vm.importarCsv(csv);
        assertEquals(1, importadas.size());
        assertEquals("Gmail", importadas.get(0).getSite());
        assertEquals("meu_email", importadas.get(0).getUsuario());
        assertEquals("Senha456#", importadas.get(0).getSenha());
        assertEquals("https://gmail.com", importadas.get(0).getUrl());
        assertEquals("nota", importadas.get(0).getNotas());
    }

    @Test
    void exportarEImportarCsvRedondo() throws Exception {
        VaultManager vm = new VaultManager(dir);
        File csv = new File(dir.toFile(), "export.csv");
        List<PasswordEntry> entradas = List.of(
                new PasswordEntry("Site, com virgula", "u\"ser", "p\"ass,123", "", "nota com\nquebra", "Banco"));

        vm.exportarCsv(csv, entradas);
        List<PasswordEntry> lidas = vm.importarCsv(csv);
        assertEquals(1, lidas.size());
        assertEquals("Site, com virgula", lidas.get(0).getSite());
        assertEquals("u\"ser", lidas.get(0).getUsuario());
        assertEquals("p\"ass,123", lidas.get(0).getSenha());
        assertEquals("nota com\nquebra", lidas.get(0).getNotas());
        assertEquals("Banco", lidas.get(0).getCategoria());
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
}
