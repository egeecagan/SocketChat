package server.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import server.Server;
import server.services.BroadcastService;

public class ClientHandler implements Runnable {

    public static String greetingKw = "haleluyah";

    private final Socket clientSocket;
    private String clientName = "unknown";

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream())
            );
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            writer.println(greetingKw);
            writer.flush();

            String ack = reader.readLine();
            if (ack == null) return; // bağlantı kopmuş olabilir

            while (true) {
                writer.println("what is your user name: ");

                String line = reader.readLine();
                if (line == null) return;

                String attemptedName = line.trim();

                if (attemptedName.isEmpty()) {
                    writer.println("username cannot be empty.");
                    continue;
                }

                if (Server.clientMap.containsKey(attemptedName)) {
                    writer.println("username already in use. try another one.");
                    continue;
                }

                synchronized (Server.clientMap) {
                    Server.clientMap.put(attemptedName, clientSocket);
                }

                clientName = attemptedName;
                BroadcastService.broadcast(clientName + " joined the chat!", clientSocket, "SERVER");
                writer.println("username accepted. welcome " + clientName + "!");
                break;
            }


            System.out.println("user connected: " + clientName);


            String time;

            String clientIP = clientSocket.getInetAddress().getHostAddress();
            int clientPort = clientSocket.getPort();
            String serverIP = clientSocket.getLocalAddress().getHostAddress();
            int serverPort = clientSocket.getLocalPort();

            writer.println(clientName + " " + clientIP + " " + clientPort);
            writer.println(Server.getServerName() + " " + serverIP + " " + serverPort);

            String input;
            while ((input = reader.readLine()) != null) {
                time = new SimpleDateFormat("HH:mm").format(new Date());

                if (input.trim().equalsIgnoreCase("/exit")) {
                    break;
                }

                System.out.println("[" + time + "] " + clientName + ": " + input);
                BroadcastService.broadcast(input, clientSocket, clientName);
            }

        } catch (IOException e) {
            System.err.println("clientHandler error (" + clientName + ") -> " + e.getMessage());
        } finally {
            Server.clientMap.remove(clientName);
            BroadcastService.broadcast(clientName + " has left the chat.", clientSocket, "SERVER");
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }
}
