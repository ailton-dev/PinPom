# Política de Segurança

## Estado de auditoria

**Aviso importante:** a criptografia deste aplicativo **não foi auditada de
forma independente** por especialistas em segurança. Use-o com essa ressalva e
mantenha backups do seu cofre.

O app usa AES-256-GCM para cifrar o cofre em disco, com chave derivada da
senha mestre via PBKDF2. A senha mestre nunca é armazenada; sem ela não há
como recuperar os dados.

## Reportando uma vulnerabilidade

Por favor, **não abra issues públicas** para vulnerabilidades de segurança.
Envie um e-mail privado para:

- **Ailton Martins** — `ailtonmartins.dev@gmail.com`

Você receberá uma confirmação de recebimento e, assim que o problema for
analisado e corrigido, será feito um aviso (advisory) com crédito ao
responsável pelo reporte, se desejado.

### O que reportar

- Qualquer forma de extrair senhas ou dados do cofre sem a senha mestre/PIN;
- Vazamento de dados via logs, memória ou arquivos temporários;
- Vulnerabilidades no tratamento de PIN, senha mestre ou chave de criptografia;
- Comportamentos que permitam força bruta sem limitação/aviso;
- Qualquer outro problema que comprometa a confidencialidade dos dados.

## Modelo de ameaça

O app assume que o computador onde o cofre está aberto é confiável. As senhas
ficam em memória apenas enquanto o cofre está desbloqueado. Ao perder o foco
da janela, as senhas são ocultadas (a menos que captura de tela esteja
habilitada nas configurações).

## Dependências

Mantenha as dependências (FlatLaf, Gson, JUnit) atualizadas e rode `mvn
verify` antes de cada release.
