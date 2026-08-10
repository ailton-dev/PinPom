#!/bin/bash
# Executa o Gerenciador de Senhas
set -e

cd "$(dirname "$0")"

if [ ! -f "gerenciador-senhas.jar" ]; then
    echo "JAR nao encontrado. Compilando primeiro..."
    ./build.sh
fi

java -jar gerenciador-senhas.jar
