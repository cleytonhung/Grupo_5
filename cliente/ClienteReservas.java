import java.net.*;
import java.io.*;
import java.util.Scanner;

/**
 * Cliente TCP para o Sistema de Reservas
 * 
 * Liga-se ao ServidorSocket via TCP e permite ao utilizador
 * gerir reservas de forma interativa.
 * 
 * Grupo 5 - Sistemas Distribuídos
 */
public class ClienteReservas {

    private final String host;
    private final int porta;

    public ClienteReservas(String host, int porta) {
        this.host = host;
        this.porta = porta;
    }

    public void iniciar() {
        System.out.println("════════════════════════════════════════════");
        System.out.println("  SISTEMA DE RESERVAS - Grupo 5");
        System.out.println("  Ligando a " + host + ":" + porta);
        System.out.println("════════════════════════════════════════════");

        try (
            Socket socket = new Socket(host, porta);
            BufferedReader servidor = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter para_servidor = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            Scanner scanner = new Scanner(System.in)
        ) {
            // Mostrar mensagem de boas-vindas do servidor
            String linha;
            while ((linha = servidor.readLine()) != null) {
                System.out.println("[Servidor] " + linha);
                if (linha.equals("---")) break;
            }

            // Loop interativo
            while (true) {
                System.out.print("\n> Comando: ");
                String comando = scanner.nextLine().trim();

                if (comando.isEmpty()) continue;

                para_servidor.println(comando);

                // Lê resposta (pode ser múltiplas linhas para LISTAR_OCUPADOS)
                StringBuilder resposta = new StringBuilder();
                while (servidor.ready() || resposta.length() == 0) {
                    String r = servidor.readLine();
                    if (r == null) break;
                    resposta.append(r).append("\n");
                    if (!servidor.ready()) break;
                }

                System.out.println("[Resposta] " + resposta.toString().trim());

                if (comando.equalsIgnoreCase("SAIR")) {
                    System.out.println("Sessão encerrada.");
                    break;
                }
            }

        } catch (ConnectException e) {
            System.err.println("ERRO: Não foi possível ligar ao servidor em " + host + ":" + porta);
        } catch (IOException e) {
            System.err.println("ERRO de ligação: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        String host = System.getenv().getOrDefault("SERVER_HOST", "localhost");
        int porta   = Integer.parseInt(System.getenv().getOrDefault("SOCKET_PORT", "5000"));

        new ClienteReservas(host, porta).iniciar();
    }
}
