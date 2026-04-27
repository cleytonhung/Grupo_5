import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface RMI para o Sistema de Reservas
 * Grupo 5 - Sistemas Distribuídos
 */
public interface ReservaService extends Remote {

    /**
     * Reserva um lugar pelo número do assento.
     * @param assento número do assento (1-50)
     * @param cliente nome do cliente
     * @return true se reserva foi bem-sucedida, false se já estava ocupado
     */
    boolean reservarLugar(int assento, String cliente) throws RemoteException;

    /**
     * Cancela a reserva de um lugar.
     * @param assento número do assento
     * @param cliente nome do cliente (deve ser o mesmo que reservou)
     * @return true se cancelamento foi bem-sucedido
     */
    boolean cancelarReserva(int assento, String cliente) throws RemoteException;

    /**
     * Retorna a lista de lugares disponíveis.
     */
    List<Integer> getLugaresDisponiveis() throws RemoteException;

    /**
     * Retorna a lista de lugares ocupados e por quem.
     */
    List<String> getLugaresOcupados() throws RemoteException;

    /**
     * Retorna o estado completo de um lugar específico.
     */
    String getEstadoLugar(int assento) throws RemoteException;

    /**
     * Retorna o número total de lugares disponíveis.
     */
    int getTotalDisponiveis() throws RemoteException;

    /**
     * Sincroniza estado com outro servidor RMI (replicação).
     * @param estado mapa serializado do estado dos assentos
     */
    void sincronizarEstado(String estadoJson) throws RemoteException;
}
