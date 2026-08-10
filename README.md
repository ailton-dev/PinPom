# Gerenciador de Senhas (Java, Linux, GUI)

Aplicativo desktop em Java (Swing) que guarda suas senhas **localmente**,
em um arquivo cifrado no seu próprio computador — nada é enviado para a
internet. O cofre é protegido por uma **senha mestre** que você cria no
primeiro uso.

## Recursos

- Cadastro, edição, remoção e busca de senhas
- Categorias para organizar as entradas (E-mail, Banco, Redes sociais...)
- Senha mestre obrigatória para abrir o aplicativo
- Armazenamento local criptografado com **AES-256-GCM**
- Chave derivada da senha mestre via **PBKDF2 (150.000 iterações)** — a
  senha mestre em si nunca é gravada em disco
- Importação de senhas a partir de arquivo **CSV**
- Exportação de senhas para **CSV**
- Gerador de senha **configurável** (tamanho, maiúsculas, minúsculas,
  números, símbolos e exclusão de caracteres ambíguos)
- Medidor de **força da senha** (entropia em bits)
- Cópia de senha para a área de transferência
- Troca de senha mestre a qualquer momento
- **Tema claro/escuro** com preferência salva
- Menu, atalhos de teclado e painel de **ajuda**
- Painel **Sobre/Criador** com links do autor

## Requisitos

- Java 17 ou superior (testado com OpenJDK 21)
- Maven 3.x para compilar (`sudo apt install maven`)
- Ambiente gráfico (X11/Wayland) — é uma aplicação desktop com interface Swing

Para instalar o JDK no Ubuntu/Debian, caso não tenha:

```bash
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk-headless maven
```

## Como compilar e executar

```bash
./build.sh   # compila, roda os testes e gera gerenciador-senhas.jar
./run.sh     # executa a aplicação (compila automaticamente se necessário)
```

Ou, manualmente:

```bash
mvn clean package
cp target/gerenciador-senhas.jar .
java -jar gerenciador-senhas.jar
```

## Testes

```bash
mvn test
```

Cobre criptografia (AES-GCM/PBKDF2), gerador de senhas, cofre
(criar/abrir/trocar senha) e CSV (importação/exportação com acentuação,
vírgulas e quebras de linha).

## Primeiro uso

1. Ao abrir pela primeira vez, o app pede para você **criar uma senha
   mestre** (mínimo de 8 caracteres). Essa senha protege todas as demais.
2. **Guarde bem essa senha.** Não existe recuperação: se você esquecê-la,
   não há como acessar as senhas salvas, pois nem mesmo o próprio
   aplicativo consegue decifrar o cofre sem ela.
3. Nas próximas vezes, basta digitar a senha mestre para entrar.

## Onde os dados ficam salvos

```
~/.gerenciador-senhas/cofre.dat
```

Esse arquivo contém tudo cifrado (site, usuário, senha, URL e notas). O
salt usado na derivação da chave fica junto no mesmo arquivo — isso é
normal e não compromete a segurança, pois sem a senha mestre correta o
conteúdo não pode ser decifrado.

## Importar de CSV

Use o botão **"Importar CSV..."**. O aplicativo reconhece automaticamente
cabeçalhos comuns, incluindo os usados por exportações de navegadores:

| Coluna reconhecida | Também aceita |
|---|---|
| site | name, title, nome |
| usuario | username, user, login |
| senha | password, pass |
| url | link, website |
| notas | note, notes, obs, observacoes |
| categoria | category, grupo, folder |

Exemplo de CSV aceito:

```csv
site,usuario,senha,url,notas,categoria
GitHub,meu_usuario,MinhaSenha123!,https://github.com,conta pessoal,Trabalho
Gmail,meu_email@gmail.com,OutraSenha456#,https://gmail.com,,E-mail
```

Se o cabeçalho não for reconhecido, o app assume a ordem padrão
`site,usuario,senha,url,notas` a partir da primeira linha.

## Exportar para CSV

Use o botão **"Exportar CSV..."**. Atenção: o arquivo exportado contém as
senhas em **texto puro** (sem criptografia), pois é o formato CSV padrão.
O app avisa sobre isso antes de exportar. Guarde o arquivo em local seguro
e apague-o quando não precisar mais.

## Estrutura do projeto

```
gerenciador-senhas/
├── pom.xml                 # build Maven (gera JAR executável com FlatLaf embutido)
├── build.sh
├── run.sh
├── README.md
└── src/
    ├── main/java/gerenciador/
    │   ├── Main.java                  # ponto de entrada
    │   ├── crypto/CryptoUtil.java     # PBKDF2 + AES-GCM
    │   ├── model/PasswordEntry.java   # modelo de uma entrada (com categoria)
    │   ├── storage/VaultManager.java  # leitura/escrita do cofre + CSV
    │   ├── ui/
    │   │   ├── LoginFrame.java        # tela de criação/entrada da senha mestre
    │   │   ├── MainFrame.java         # janela principal (tabela + ações + menu)
    │   │   ├── EntryDialog.java       # diálogo de adicionar/editar entrada
    │   │   ├── PasswordGeneratorDialog.java  # gerador de senha configurável
    │   │   ├── AboutDialog.java       # painel do criador (GitHub, Instagram, e-mail, Reddit)
    │   │   ├── HelpDialog.java        # painel de ajuda
    │   │   └── ThemeUtil.java         # tema claro/escuro (FlatLaf)
    │   └── util/
    │       ├── PasswordGenerator.java # gerador de senha + cálculo de entropia
    │       ├── ConfigStore.java       # preferências (tema) em config.properties
    │       └── CriadorLinks.java      # links do autor
    └── test/java/gerenciador/         # testes JUnit 5
        ├── crypto/CryptoUtilTest.java
        ├── storage/VaultManagerTest.java
        └── util/PasswordGeneratorTest.java
```

## Atalhos de teclado

| Atalho   | Ação                                   |
|----------|----------------------------------------|
| Ctrl+N   | Nova entrada                           |
| F2       | Editar entrada selecionada             |
| Delete   | Remover entrada selecionada            |
| Ctrl+C   | Copiar senha                           |
| Ctrl+D   | Duplicar entrada selecionada           |
| Ctrl+F   | Focar na busca                         |
| Ctrl+,   | Configurações                          |
| Ctrl+L   | Bloquear (voltar para a senha mestre)  |
| Ctrl+T   | Alternar tema claro/escuro             |
| F1       | Abrir a ajuda                          |


## Notas de segurança

- A senha mestre nunca é armazenada — apenas usada para derivar a chave
  de criptografia toda vez que o cofre é aberto.
- O modo AES-GCM garante integridade: se o arquivo for adulterado ou a
  senha estiver errada, a descriptografia falha explicitamente.
- Este é um projeto local e educacional; para uso crítico/corporativo,
  considere soluções auditadas como KeePassXC ou Bitwarden.
