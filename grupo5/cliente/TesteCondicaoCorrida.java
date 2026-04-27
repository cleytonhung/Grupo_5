import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Teste de Condições de Corrida
 * 
 * Simula múltiplos clientes a tentar reservar o MESMO assento
 * em simultâneo para demonstrar que o sistema evita condições de corrida.
 * 
 * Grupo 5 - Sistemas Distribuídos
 */
public class TesteCondicaoCorrida {

    private static final AtomicInteger sucessos = new AtomicInteger(0);
    private static final AtomicInteger falhas   = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        String host          = System.getenv().getOrDefault("SERVER_HOST", "localhost");
        int porta            = Integer.parseInt(System.getenv().getOrDefault("SOCKET_PORT", "5000"));
        int numClientes      = Integer.parseInt(System.getenv().getOrDefault("NUM_CLIENTES", "10"));
        int assentoAlvo      = 1; // Todos tentam reservar o mesmo assento

        System.out.println("════════════════════════════════════════════");
        System.out.println("  TESTE DE CONDIÇÃO DE CORRIDA");
        System.out.println("  " + numClientes + " clientes tentam reservar o assento " + assentoAlvo);
        System.out.println("  Servidor: " + host + ":" + porta);
        System.out.println("════════════════════════════════════════════\n");

        ExecutorService pool = Executors.newFixedThreadPool(numClientes);
        CountDownLatch inicio = new CountDownLatch(1); // Todos partem ao mesmo tempo
        CountDownLatch fim    = new CountDownLatch(numClientes);

        for (int i = 0; i < numClientes; i++) {
            final String nomeCliente = "Cliente_" + (i + 1);
            pool.submit(() -> {
                try {
                    inicio.await(); // Espera sinal de arranque
                    tentarReservar(host, porta, assentoAlvo, nomeCliente);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    fim.countDown();
                }
            });
        }

        System.out.println(">> Disparando todos os clientes em simultâneo...\n");
        inicio.countDown(); // DISPARA TODOS AO MESMO TEMPO
        fim.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        System.out.println("\n════════════════════════════════════════════");
        System.out.println("  RESULTADO DO TESTE");
        System.out.println("  Reservas bem-sucedidas: " + sucessos.get() + " (esperado: 1)");
        System.out.println("  Reservas recusadas:     " + falhas.get()   + " (esperado: " + (numClientes - 1) + ")");

        if (sucessos.get() == 1) {
            System.out.println("\n  ✅ PASSOU: Sistema evitou condição de corrida!");
        } else {
            System.out.println("\n  ❌ FALHOU: " + sucessos.get() + " reservas do mesmo assento!");
        }
        System.out.println("════════════════════════════════════════════");
    }

    private static void tentarReservar(String host, int porta, int assento, String cliente) {
        try (
            Socket s    = new Socket(host, porta);
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            PrintWriter pw    = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true)
        ) {
            // Consome mensagem de boas-vindas
            while (br.ready()) br.readLine();
            Thread.sleep(100); // Deixa welcome msg chegar
            while (br.ready()) br.readLine();

            // Envia comando de reserva
            pw.println("RESERVAR:" + assento + ":" + cliente);
            Thread.sleep(500);

            StringBuilder resp = new StringBuilder();
            while (br.ready()) {
                resp.append(br.readLine()).append(" ");
            }

            String resposta = resp.toString().trim();
            System.out.println("[" + cliente + "] Resposta: " + resposta);

            if (resposta.startsWith("OK")) {
                sucessos.incrementAndGet();
            } else {
                falhas.incrementAndGet();
            }

            pw.println("SAIR");

        } catch (Exception e) {
            System.err.println("[" + cliente + "] ERRO: " + e.getMessage());
            falhas.incrementAndGet();
        }
    }
}
