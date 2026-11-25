#!/bin/bash

echo "=== Configuração e Arranque do Tomcat (Gestão de Tarefas) ==="

# 1. Copiar o driver do MySQL para a pasta lib do Tomcat
TOMCAT_LIB="/opt/homebrew/opt/tomcat/libexec/lib"
MYSQL_JAR="$(pwd)/lib/mysql-connector-j-26.7.0.jar"

if [ -f "$MYSQL_JAR" ]; then
    echo "1. A copiar o driver MySQL para o Tomcat..."
    cp "$MYSQL_JAR" "$TOMCAT_LIB/"
    echo "   -> Driver copiado com sucesso!"
else
    echo "   [!] Ficheiro mysql-connector-j não encontrado na pasta lib local."
fi

# 2. Criar atalho (symlink) da aplicação para a pasta webapps do Tomcat
TOMCAT_WEBAPPS="/opt/homebrew/opt/tomcat/libexec/webapps"
APP_DIR="$(pwd)/webapp/tarefas/servidor-tomcat"

if [ ! -e "$TOMCAT_WEBAPPS/tarefas" ]; then
    echo "2. A ligar a aplicação tarefas ao Tomcat..."
    ln -s "$APP_DIR" "$TOMCAT_WEBAPPS/tarefas"
    echo "   -> Ligação criada com sucesso!"
else
    echo "2. A aplicação já estava ligada ao Tomcat."
fi

# 3. Iniciar o MySQL se não estiver ativo
echo "3. A verificar o MySQL..."
mysql.server start

# 4. Iniciar o Tomcat
echo "4. A iniciar o Apache Tomcat..."
catalina start

echo ""
echo "=== TUDO PRONTO! ==="
echo "Podes aceder no navegador a: http://localhost:8088/tarefas/"
