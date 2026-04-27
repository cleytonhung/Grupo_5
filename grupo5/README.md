# Grupo 5 — Sistema de Reserva de Lugares (Cinema/Voo)
**Curso de Licenciatura em Informática — Sistemas Distribuídos**

---

## Arquitetura

```
┌──────────────────────────────────────────────────────────────┐
│                       DOCKER NETWORK                         │
│                                                              │
│  ┌─────────────────┐   RMI (replicação)  ┌────────────────┐ │
│  │   servidor1     │ ◄──────────────────► │   servidor2    │ │
│  │                 │                      │                │ │
│  │ ServidorRMI     │                      │ ServidorRMI    │ │
│  │ (porta 1099)    │                      │ (porta 1099)   │ │
│  │                 │                      │                │ │
│  │ ServidorSocket  │                      │ ServidorSocket │ │
│  │ (porta 5000)    │                      │ (porta 5000)   │ │
│  └────────┬────────┘                      └────────────────┘ │
│           │ TCP                                               │
│  ┌────────▼────────┐                                         │
│  │    cliente      │                                         │
│  │  ClienteTCP     │                                         │
│  └─────────────────┘                                         │
└──────────────────────────────────────────────────────────────┘
```

## Como executar

### 1. Clonar o repositório
```bash
git clone <url-do-repositorio>
cd grupo5
```

### 2. Iniciar os servidores e teste automático
```bash
docker-compose up --build
```
Isto irá:
- Compilar e iniciar `servidor1` (primário) e `servidor2` (réplica)
- Executar automaticamente o teste de condição de corrida com 15 clientes simultâneos

### 3. Cliente interativo
```bash
# Num terminal separado
docker-compose --profile interativo run cliente
```

#### Comandos disponíveis:
| Comando | Descrição |
|---------|-----------|
| `RESERVAR:5:João` | Reserva o assento 5 para João |
| `CANCELAR:5:João` | Cancela a reserva do assento 5 |
| `LISTAR_DISPONIVEIS` | Lista todos os assentos disponíveis |
| `LISTAR_OCUPADOS` | Lista todos os assentos ocupados |
| `ESTADO:5` | Estado do assento 5 |
| `TOTAL_DISPONIVEIS` | Total de assentos livres |
| `SAIR` | Encerra a ligação |

### 4. Ligar diretamente (sem Docker)
```bash
# Compilar
cd servidor
javac ReservaService.java ServidorRMI.java ServidorSocket.java

# Iniciar servidor RMI
java ServidorRMI &

# Iniciar servidor Socket
java ServidorSocket &

# Num outro terminal, iniciar cliente
cd ../cliente
javac ReservaService.java ClienteReservas.java TesteCondicaoCorrida.java
java ClienteReservas
```

---

## Mecanismos Anti-Condição de Corrida

1. **`synchronized`** nos métodos `reservarLugar` e `cancelarReserva` — garante exclusão mútua
2. **`ConcurrentHashMap`** — estrutura thread-safe para acesso concorrente ao estado
3. **Replicação assíncrona** via RMI — mantém consistência entre os dois servidores

---

## Entregáveis

- [x] Repositório GitHub com histórico de commits
- [x] Código-fonte completo (Java + Docker)
- [x] `docker-compose.yml` com dois servidores
- [ ] Relatório técnico (ver `relatorio/relatorio_tecnico.pdf`)

**Data de Entrega:** 7 de Maio de 2026  
**Partilha do repositório:** até 27 de Abril de 2026
