FROM openjdk:17-jdk-slim

WORKDIR /app

# Copia os ficheiros fonte
COPY ReservaService.java .
COPY ServidorRMI.java .
COPY ServidorSocket.java .

# Compila todos os ficheiros
RUN javac ReservaService.java ServidorRMI.java ServidorSocket.java

# Variáveis de ambiente (podem ser sobrescritas no docker-compose)
ENV SERVER_NAME=Servidor1
ENV RMI_PORT=1099
ENV RMI_HOST=servidor1
ENV SOCKET_PORT=5000
ENV RMI_URL=//localhost:1099/ReservaService
ENV SECONDARY_RMI_URL=""

# Expõe as portas RMI e Socket
EXPOSE 1099 5000

# Script de arranque: lança RMI em background, depois Socket
CMD sh -c "\
    java -Djava.rmi.server.hostname=${RMI_HOST} \
         -DSERVER_NAME=${SERVER_NAME} \
         ServidorRMI & \
    sleep 3 && \
    java -DSOCKET_PORT=${SOCKET_PORT} \
         -DRMI_URL=${RMI_URL} \
         ServidorSocket"
