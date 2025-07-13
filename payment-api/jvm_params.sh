#!/bin/bash

# --- Variáveis de Configuração ---
# O caminho para o seu JAR executável do Quarkus
APP_JAR="/app/quarkus-app/quarkus-run.jar"

# --- Configurações da JVM para Recursos Extremamente Limitados ---
# (0.6 CPU, 158MB RAM)

# Define o máximo de memória RAM que a JVM pode usar, em % do limite do contêiner.
# 70% de 158MB = ~110MB para a heap.
export JAVA_MAX_MEM_RATIO="70"

# Define a memória inicial da heap como 70% da memória máxima.
# Isso evita redimensionamentos custosos da heap em tempo de execução.
export JAVA_INITIAL_MEM_RATIO="70"

# Define o limite máximo para a memória inicial da heap em MB.
# Garante que não ultrapasse um limite razoável para 158MB totais.
export JAVA_MAX_INITIAL_MEM="100"

# Opções adicionais da JVM.
# -Xss512k: Reduz o tamanho da pilha de cada thread para economizar memória.
# -XX:+UseSerialGC: O coletor de lixo SerialGC é o mais simples e consome menos recursos
#                   de CPU/memória para o próprio GC, ideal para ambientes muito limitados.
# -XX:MetaspaceSize=32M: Define o tamanho inicial do Metaspace.
# -XX:MaxMetaspaceSize=64M: Define o tamanho máximo do Metaspace.
# -XX:+ExitOnOutOfMemoryError: Garante que a JVM saia em caso de OOM para que o contêiner possa ser reiniciado.
export JAVA_OPTS_APPEND="-Xss512k -XX:+UseSerialGC -XX:MetaspaceSize=32M -XX:MaxMetaspaceSize=64M -XX:+ExitOnOutOfMemoryError"

# Informa ao Quarkus/JVM o limite de CPU do contêiner.
# Ajuda o Vert.x a ajustar seus pools de threads.
export CONTAINER_CORE_LIMIT="0.6"

# --- Execução da Aplicação ---
echo "Iniciando a aplicação Java com as seguintes configurações:"
echo "  APP_JAR: ${APP_JAR}"
echo "  JAVA_MAX_MEM_RATIO: ${JAVA_MAX_MEM_RATIO}"
echo "  JAVA_INITIAL_MEM_RATIO: ${JAVA_INITIAL_MEM_RATIO}"
echo "  JAVA_MAX_INITIAL_MEM: ${JAVA_MAX_INITIAL_MEM}"
echo "  JAVA_OPTS_APPEND: ${JAVA_OPTS_APPEND}"
echo "  CONTAINER_CORE_LIMIT: ${CONTAINER_CORE_LIMIT}"
echo "------------------------------------------------------------"

# Executa o JAR. Note que o script 'run-java.sh' (comumente usado em imagens base OpenJDK/Quarkus)
# já interpreta essas variáveis de ambiente e constrói o comando 'java' apropriadamente.
# Se você não estiver usando um script de entrada que interpreta essas variáveis,
# você precisaria construir o comando 'java' explicitamente aqui.
# Por exemplo: java -Xmx${MAX_MEM} -Xms${INITIAL_MEM} -jar ${APP_JAR} ${JAVA_OPTS_APPEND}
# Mas para o contexto de Quarkus e contêineres, o padrão é usar um script wrapper.
exec java -jar ${APP_JAR}