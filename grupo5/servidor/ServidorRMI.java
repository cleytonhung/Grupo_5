import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.Naming;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Implementação do Servidor RMI de Reservas
 * 
 * Mecanismos anti-condição de corrida:
 * - synchronized em métodos críticos (reservar/cancelar)
 * - ConcurrentHashMap para estado dos assentos
 * - Replicação de estado para servidor secundário
 * 
 * Grupo 5 - Sistemas Distribuídos
 */
public class ServidorRMI extends UnicastRemoteObject implements ReservaService {

    private static final Logger logger = Logger.getLogger(ServidorRMI.class.getName());
    private static final int TOTAL_ASSENTOS = 50;

    // ConcurrentHashMap: thread-safe para leituras concorrentes
    // null = disponível, String = nome do cliente
    private final ConcurrentHashMap<Integer, String> assentos = new ConcurrentHashMap<>();

    private String servidorSecundario = null;
    private String nomeServidor;

    public ServidorRMI(String nomeServidor) throws RemoteException {
        super();
        this.nomeServidor = nomeServidor;
        // Inicializa todos os assentos como disponíveis
        for (int i = 1; i <= TOTAL_ASSENTOS; i++) {
            assentos.put(i, null);
        }
        logger.info("[" + nomeServidor + "] Servidor RMI inicializado com " + TOTAL_ASSENTOS + " assentos.");
    }

    /**
     * MÉTODO CRÍTICO - synchronized para evitar condições de corrida.
     * Dois clientes não podem reservar o mesmo assento simultaneamente.
     */
    @Override
    public synchronized boolean reservarLugar(int assento, String cliente) throws RemoteException {
        if (assento < 1 || assento > TOTAL_ASSENTOS) {
            logger.warning("[" + nomeServidor + "] Assento inválido: " + assento);
            return false;
        }

        String ocupante = assentos.get(assento);

        if (ocupante != null) {
            logger.info("[" + nomeServidor + "] Assento " + assento + " já ocupado por " + ocupante +
                    ". Recusado para: " + cliente);
            return false;
        }

        // Reserva o assento
        assentos.put(assento, cliente);
        logger.info("[" + nomeServidor + "] ✓ Assento " + assento + " reservado para: " + cliente);

        // Replicar estado para servidor secundário
        replicarParaSecundario();

        return true;
    }

    /**
     * MÉTODO CRÍTICO - synchronized para cancelamentos seguros.
     */
    @Override
    public synchronized boolean cancelarReserva(int assento, String cliente) throws RemoteException {
        if (assento < 1 || assento > TOTAL_ASSENTOS) {
            return false;
        }

        String ocupante = assentos.get(assento);

        if (ocupante == null) {
            logger.info("[" + nomeServidor + "] Assento " + assento + " já está livre.");
            return false;
        }

        if (!ocupante.equals(cliente)) {
            logger.warning("[" + nomeServidor + "] Cliente " + cliente +
                    " tentou cancelar reserva de " + ocupante + " no assento " + assento);
            return false;
        }

        assentos.put(assento, null);
        logger.info("[" + nomeServidor + "] ✓ Reserva do assento " + assento + " cancelada para: " + cliente);

        replicarParaSecundario();
        return true;
    }

    @Override
    public List<Integer> getLugaresDisponiveis() throws RemoteException {
        List<Integer> disponiveis = new ArrayList<>();
        for (int i = 1; i <= TOTAL_ASSENTOS; i++) {
            if (assentos.get(i) == null) {
                disponiveis.add(i);
            }
        }
        Collections.sort(disponiveis);
        return disponiveis;
    }

    @Override
    public List<String> getLugaresOcupados() throws RemoteException {
        List<String> ocupados = new ArrayList<>();
        for (int i = 1; i <= TOTAL_ASSENTOS; i++) {
            String ocupante = assentos.get(i);
            if (ocupante != null) {
                ocupados.add("Assento " + i + " -> " + ocupante);
            }
        }
        return ocupados;
    }

    @Override
    public String getEstadoLugar(int assento) throws RemoteException {
        if (assento < 1 || assento > TOTAL_ASSENTOS) {
            return "Assento inválido";
        }
        String ocupante = assentos.get(assento);
        return ocupante == null ? "Assento " + assento + ": DISPONÍVEL"
                                : "Assento " + assento + ": OCUPADO por " + ocupante;
    }

    @Override
    public int getTotalDisponiveis() throws RemoteException {
        int count = 0;
        for (int i = 1; i <= TOTAL_ASSENTOS; i++) {
            if (assentos.get(i) == null) count++;
        }
        return count;
    }

    /**
     * Recebe e aplica estado replicado do servidor primário.
     */
    @Override
    public synchronized void sincronizarEstado(String estadoJson) throws RemoteException {
        // Formato simples: "1:João,2:Maria,5:null,..."
        logger.info("[" + nomeServidor + "] Sincronizando estado recebido...");
        String[] entradas = estadoJson.split(",");
        for (String entrada : entradas) {
            String[] partes = entrada.split(":");
            if (partes.length == 2) {
                int num = Integer.parseInt(partes[0].trim());
                String valor = partes[1].trim();
                assentos.put(num, valor.equals("null") ? null : valor);
            }
        }
        logger.info("[" + nomeServidor + "] Estado sincronizado com sucesso.");
    }

    /**
     * Configura o endereço do servidor secundário para replicação.
     */
    public void setServidorSecundario(String enderecoRMI) {
        this.servidorSecundario = enderecoRMI;
        logger.info("[" + nomeServidor + "] Servidor secundário configurado: " + enderecoRMI);
    }

    /**
     * Serializa estado atual para JSON simplificado.
     */
    private String serializarEstado() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= TOTAL_ASSENTOS; i++) {
            if (sb.length() > 0) sb.append(",");
            String ocupante = assentos.get(i);
            sb.append(i).append(":").append(ocupante == null ? "null" : ocupante);
        }
        return sb.toString();
    }

    /**
     * Replica o estado atual para o servidor secundário.
     * Falha silenciosa - não bloqueia operação principal.
     */
    private void replicarParaSecundario() {
        if (servidorSecundario == null) return;
        new Thread(() -> {
            try {
                ReservaService secundario = (ReservaService) Naming.lookup(servidorSecundario);
                secundario.sincronizarEstado(serializarEstado());
                logger.info("[" + nomeServidor + "] Estado replicado para servidor secundário.");
            } catch (Exception e) {
                logger.warning("[" + nomeServidor + "] Aviso: não foi possível replicar para secundário: " + e.getMessage());
            }
        }).start();
    }

    // ─── MAIN ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        String nome         = System.getenv().getOrDefault("SERVER_NAME", "Servidor1");
        int porta           = Integer.parseInt(System.getenv().getOrDefault("RMI_PORT", "1099"));
        String secundarioUrl = System.getenv("SECONDARY_RMI_URL"); // ex: //servidor2:1099/ReservaService

        System.setProperty("java.rmi.server.hostname",
                System.getenv().getOrDefault("RMI_HOST", "localhost"));

        ServidorRMI servidor = new ServidorRMI(nome);

        if (secundarioUrl != null && !secundarioUrl.isEmpty()) {
            servidor.setServidorSecundario(secundarioUrl);
        }

        Registry registry = LocateRegistry.createRegistry(porta);
        registry.rebind("ReservaService", servidor);

        System.out.println("════════════════════════════════════════════");
        System.out.println("  [" + nome + "] Servidor RMI ativo na porta " + porta);
        System.out.println("  Assentos disponíveis: " + TOTAL_ASSENTOS);
        System.out.println("  Replicação para: " + (secundarioUrl != null ? secundarioUrl : "desativada"));
        System.out.println("════════════════════════════════════════════");
    }
}
