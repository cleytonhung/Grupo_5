FROM openjdk:17-jdk-slim

WORKDIR /app

# Copia os ficheiros fonte
COPY ../servidor/ReservaService.java .
COPY ClienteReservas.java .
COPY TesteCondicaoCorrida.java .

# Compila
RUN javac ReservaService.java ClienteReservas.java TesteCondicaoCorrida.java

# Variáveis padrão
ENV SERVER_HOST=servidor1
ENV SOCKET_PORT=5000
ENV NUM_CLIENTES=10

# Por padrão executa o teste de condição de corrida
CMD ["java", "TesteCondicaoCorrida"]
