# Grupo 5 — Sistema de Reserva de Lugares

# Nomes:
- Américo Baptista
- Cleyton Hung
- Tarcisio Mucaverde

## Tema
Sistema de Reserva de Lugares para Cinema ou Voo.

## Objetivo
Desenvolver um sistema capaz de evitar condições de corrida durante reservas simultâneas de lugares, garantindo que dois clientes não consigam reservar o mesmo lugar ao mesmo tempo.

## Tecnologias Utilizadas
- Java
- RMI
- Sockets TCP
- Docker
- Docker Compose

## Funcionamento do Sistema
O sistema possui servidores responsáveis por gerir os lugares disponíveis.  
O cliente liga-se ao servidor através de Socket TCP para consultar lugares e fazer reservas.

Quando um lugar é reservado, o servidor atualiza o estado da reserva e sincroniza a informação com outros servidores usando RMI.

Para evitar conflitos em reservas simultâneas, são usados mecanismos como `synchronized` ou `locks`.

## Estrutura do Projeto

```text
grupo5/
├── docker-compose.yml
├── servidor/
│   ├── ServidorRMI.java
│   ├── GestorReservas.java
│   └── Dockerfile
├── cliente/
│   ├── ClienteSocket.java
│   └── Dockerfile
└── relatorio/
    └── relatorio_tecnico.pdf
