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

                String line = reader.readLine();
                if (line == null) return;

                String attemptedName = line.trim();

                if (attemptedName.isEmpty()) {
                    writer.println("username cannot be empty");
                    continue;
                }

                if (Server.clientMap.containsKey(attemptedName)) {
                    writer.println("username already in use try another one");
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
                } else if (input.trim().startsWith("/rename ")) {
                    String[] parts = input.trim().split("\\s+", 2);
                    if (parts.length < 2 || parts[1].trim().isEmpty()) {
                        writer.println("usage: /rename <new_name>");
                        continue;
                    }
                    String newName = parts[1].trim();
                    if (Server.clientMap.containsKey(newName)) {
                        writer.println("username already in use. try another one.");
                        continue;
                    }
                    synchronized (Server.clientMap) {
                        Server.clientMap.remove(clientName);
                        Server.clientMap.put(newName, clientSocket);
                    }
                    BroadcastService.broadcast(clientName + " is now known as " + newName, clientSocket, "SERVER");
                    clientName = newName;
                    writer.println("your username is now: " + clientName);
                } else if (input.trim().startsWith("/w ")) {
                    /*
                     * 
                     * 
                     *  implement whisper command here
                     * 
                     * 
                     */
                } else if (input.trim().equalsIgnoreCase("/list")) {
                    int i = 0;
                    for (String name : Server.clientMap.keySet()) {
                        i++;
                        writer.println(i + " > " + name);
                    }
                } else if (input.trim().equalsIgnoreCase("/ping")) {
                    writer.println("SERVER: pong (" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + ")");
                } else if (input.trim().equalsIgnoreCase("/help")) {
                    writer.println("available commands:");
                    writer.println("/exit                 - disconnect");
                    writer.println("/rename <new_name>    - change your username");
                    writer.println("/whisper <u> <msg>    - send private message");
                    writer.println("/list                 - show online users");
                    writer.println("/ping                 - check connection");
                    writer.println("/help                 - show this message");
                } else if (input.trim().equalsIgnoreCase("/whoami")) {
                    writer.println("username            -> " + clientName);
                    writer.println("your IP address     -> " + clientIP);
                    writer.println("your port           -> " + clientPort);
                    writer.println("connected to server -> " + Server.getServerName());
                    writer.println("server IP address   -> " + serverIP);
                    writer.println("server port         -> " + serverPort);
                } else if (input.trim().startsWith("/")) {
                    writer.println("unknown command use /help");
                } else {
                    System.out.println("[" + time + "] " + clientName + ": " + input);
                    BroadcastService.broadcast(input, clientSocket, clientName);
                }
            }

        } catch (IOException e) {
            System.err.println("clientHandler error (" + clientName + ") -> " + e.getMessage());
        } finally {
            if (clientName != null && Server.clientMap.containsKey(clientName)) {
                Server.clientMap.remove(clientName);
                BroadcastService.broadcast(clientName + " has left the chat", clientSocket, "SERVER");
                System.out.println("[" + new SimpleDateFormat("HH:mm").format(new Date()) + "] " + clientName + " disconnected");
            } else {
                System.out.println("client disconnected before registering username");
            }

            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }
}
