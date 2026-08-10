#!/bin/bash
# Compila o projeto (Maven) e copia o JAR executavel para a raiz.
set -e

cd "$(dirname "$0")"

if ! command -v mvn >/dev/null 2>&1; then
    echo "Erro: Maven (mvn) nao encontrado. Instale com: sudo apt install maven" >&2
    exit 1
fi

echo "Compilando e executando testes..."
mvn -q clean package

echo "Copiando JAR para a raiz do projeto..."
cp target/gerenciador-senhas.jar ./gerenciador-senhas.jar

echo "Pronto! Gerado: gerenciador-senhas.jar"
echo "Execute com: ./run.sh   (ou: java -jar gerenciador-senhas.jar)"
