import java.net.*;
import java.io.*;
import java.rmi.Naming;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Servidor de Sockets TCP
 * 
 * Recebe ligações dos clientes via TCP e delega as operações
 * ao ServidorRMI (que gere o estado e sincronização).
 * 
 * Protocolo de comunicação (texto simples):
 *   RESERVAR:<assento>:<cliente>
 *   CANCELAR:<assento>:<cliente>
 *   LISTAR_DISPONIVEIS
 *   LISTAR_OCUPADOS
 *   ESTADO:<assento>
 *   TOTAL_DISPONIVEIS
 *   SAIR
 * 
 * Grupo 5 - Sistemas Distribuídos
 */
public class ServidorSocket {

    private static final Logger logger = Logger.getLogger(ServidorSocket.class.getName());

    private final int porta;
    private final String rmiUrl;
    private final ExecutorService threadPool;
    private ReservaService rmiServico;

    public ServidorSocket(int porta, String rmiUrl) {
        this.porta = porta;
        this.rmiUrl = rmiUrl;
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void iniciar() {
        // Aguarda o servidor RMI estar disponível
        conectarRMI();

        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("[ServidorSocket] À escuta na porta " + porta);
            System.out.println("[ServidorSocket] Conectado ao RMI: " + rmiUrl);

            while (true) {
                Socket clienteSocket = serverSocket.accept();
                String enderecoCliente = clienteSocket.getInetAddress().getHostAddress();
                logger.info("[ServidorSocket] Nova ligação de: " + enderecoCliente);
                threadPool.submit(new TratadorCliente(clienteSocket, rmiServico, enderecoCliente));
            }

        } catch (IOException e) {
            logger.severe("[ServidorSocket] Erro fatal: " + e.getMessage());
        }
    }

    private void conectarRMI() {
        int tentativas = 0;
        while (rmiServico == null && tentativas < 10) {
            try {
                tentativas++;
                System.out.println("[ServidorSocket] A ligar ao RMI... tentativa " + tentativas);
                Thread.sleep(2000);
                rmiServico = (ReservaService) Naming.lookup(rmiUrl);
                System.out.println("[ServidorSocket] ✓ Ligado ao RMI com sucesso.");
            } catch (Exception e) {
                logger.warning("[ServidorSocket] RMI não disponível ainda: " + e.getMessage());
            }
        }
        if (rmiServico == null) {
            throw new RuntimeException("Não foi possível ligar ao servidor RMI após " + tentativas + " tentativas.");
        }
    }

    public static void main(String[] args) throws Exception {
        int porta   = Integer.parseInt(System.getenv().getOrDefault("SOCKET_PORT", "5000"));
        String rmi  = System.getenv().getOrDefault("RMI_URL", "//localhost:1099/ReservaService");

        new ServidorSocket(porta, rmi).iniciar();
    }
}

// ─── Tratador por cliente ────────────────────────────────────────────────────

class TratadorCliente implements Runnable {

    private final Socket socket;
    private final ReservaService rmi;
    private final String enderecoCliente;

    TratadorCliente(Socket socket, ReservaService rmi, String enderecoCliente) {
        this.socket = socket;
        this.rmi = rmi;
        this.enderecoCliente = enderecoCliente;
    }

    @Override
    public void run() {
        try (
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter saida      = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            saida.println("BEM-VINDO ao Sistema de Reservas | Grupo 5");
            saida.println("Comandos: RESERVAR:<assento>:<nome> | CANCELAR:<assento>:<nome> | LISTAR_DISPONIVEIS | LISTAR_OCUPADOS | ESTADO:<assento> | SAIR");
            saida.println("---");

            String linha;
            while ((linha = entrada.readLine()) != null) {
                linha = linha.trim();
                if (linha.isEmpty()) continue;
                
                String resposta = processarComando(linha);
                saida.println(resposta);

                if (linha.equalsIgnoreCase("SAIR")) break;
            }

        } catch (IOException e) {
            System.err.println("[TratadorCliente] Ligação encerrada: " + enderecoCliente);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private String processarComando(String comando) {
        try {
            String[] partes = comando.split(":");

            switch (partes[0].toUpperCase()) {

                case "RESERVAR": {
                    if (partes.length < 3)
                        return "ERRO: Formato inválido. Use RESERVAR:<assento>:<nome>";
                    int assento = Integer.parseInt(partes[1].trim());
                    String cliente = partes[2].trim();
                    boolean sucesso = rmi.reservarLugar(assento, cliente);
                    return sucesso
                        ? "OK: Assento " + assento + " reservado para " + cliente
                        : "FALHA: Assento " + assento + " já está ocupado ou inválido";
                }

                case "CANCELAR": {
                    if (partes.length < 3)
                        return "ERRO: Formato inválido. Use CANCELAR:<assento>:<nome>";
                    int assento = Integer.parseInt(partes[1].trim());
                    String cliente = partes[2].trim();
                    boolean sucesso = rmi.cancelarReserva(assento, cliente);
                    return sucesso
                        ? "OK: Reserva do assento " + assento + " cancelada"
                        : "FALHA: Não foi possível cancelar (assento livre, inválido ou cliente errado)";
                }

                case "LISTAR_DISPONIVEIS": {
                    List<Integer> disp = rmi.getLugaresDisponiveis();
                    return "DISPONIVEIS (" + disp.size() + "): " + disp;
                }

                case "LISTAR_OCUPADOS": {
                    List<String> ocup = rmi.getLugaresOcupados();
                    if (ocup.isEmpty()) return "OCUPADOS: nenhum";
                    return "OCUPADOS:\n" + String.join("\n", ocup);
                }

                case "ESTADO": {
                    if (partes.length < 2)
                        return "ERRO: Formato inválido. Use ESTADO:<assento>";
                    int assento = Integer.parseInt(partes[1].trim());
                    return rmi.getEstadoLugar(assento);
                }

                case "TOTAL_DISPONIVEIS": {
                    return "TOTAL DISPONÍVEIS: " + rmi.getTotalDisponiveis();
                }

                case "SAIR":
                    return "ATE LOGO!";

                default:
                    return "ERRO: Comando desconhecido: " + partes[0];
            }

        } catch (NumberFormatException e) {
            return "ERRO: Número de assento inválido";
        } catch (Exception e) {
            return "ERRO interno: " + e.getMessage();
        }
    }
}
