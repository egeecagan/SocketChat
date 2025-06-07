package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import server.handler.ClientHandler;

public class Server {

    public static final String RESET   = "\u001B[0m";
    public static final String GREEN   = "\u001B[32m";
    public static final String RED     = "\u001B[31m";
    public static final String YELLOW  = "\u001B[33m";

    private static Server singleInstance;
    private static ServerSocket serverSocket = null;
    private static final Scanner sc = new Scanner(System.in);

    private static String serverName = "unknown";
    private static int PORT = 49153;

    public static final Map<String, Socket> clientMap = new ConcurrentHashMap<>();

    private Server() {
        try {
            if (System.console() != null) {
                askServerInfo();
            } else {
                System.out.println("non interactive environment detected using default values");
                serverName = "default";
                PORT = 49153;
            }
        } catch (Exception e) {
            System.out.println("input unavailable, using default settings");
            serverName = "default";
            PORT = 49153;
        }
    }

    public static String getServerName() {
        return serverName;
    }

    public static Server getInstance() {
        if (singleInstance == null) {
            synchronized (Server.class) {
                if (singleInstance == null) {
                    singleInstance = new Server();
                }
            }
        }
        return singleInstance;
    }

    private void askServerInfo() {
        while (true) {
            System.out.print("enter the server name -> ");
            String tempName = sc.nextLine();

            if (tempName.trim().replaceAll("\\s+", "").isEmpty()) {
                System.out.println("server name cannot be empty or just whitespace. try again.");
            } else {
                serverName = tempName.trim().replaceAll("\\s+", "");
                System.out.println("server name is -> " + serverName);
                break;
            }
        }

        System.out.println("please enter a custom port (49152 - 65535)");
        System.out.println("enter -1 to use default port 49153");

        while (true) {
            System.out.print("port -> ");
            String line = sc.nextLine().trim();

            try {
                int enteredPort = Integer.parseInt(line);

                if (enteredPort == -1) {
                    PORT = 49153;
                    System.out.println("using default port 49153");
                    break;
                } else if (enteredPort >= 49152 && enteredPort <= 65535) {
                    PORT = enteredPort;
                    System.out.println("using custom port -> " + PORT);
                    break;
                } else {
                    System.out.println("port must be between 49152 and 65535 or -1 for default. try again.");
                }

            } catch (NumberFormatException e) {
                System.out.println("invalid input. please enter a number.");
            }
        }
    }

    public int start() {
        System.out.println("auto: trying to open server socket on port " + PORT);

        Thread commandThread = new Thread(() -> {
            Scanner commandScanner = new Scanner(System.in);
            while (true) {
                System.out.print("[admin] > ");
                String command = commandScanner.nextLine().trim();

                if (command.startsWith("/greeting ")) {
                    String newGreeting = command.substring(10).trim();
                    if (!newGreeting.isEmpty()) {
                        ClientHandler.greetingKw = newGreeting.replaceAll("\\s+", "");
                        System.out.println("greeting keyword updated to: " + newGreeting);
                    } else {
                        System.out.println("usage: /greeting <new_greeting>");
                    }
                } else if (command.equalsIgnoreCase("/clientnum")) {
                    System.out.println("connected clients: " + clientMap.size());
                } else if (command.equalsIgnoreCase("/whoami")) {
                    try {
                        String localIP = java.net.InetAddress.getLocalHost().getHostAddress();
                        System.out.println("server name       -> " + serverName);
                        System.out.println("server ip address -> " + localIP);
                        System.out.println("server port       -> " + PORT);
                        System.out.println("client count      -> " + clientMap.size());
                        System.out.println("listening         -> " + (serverSocket != null && !serverSocket.isClosed()));
                        System.out.println("greeting keyword  -> " + ClientHandler.greetingKw);
                    } catch (Exception e) {
                        System.out.println("failed to retrieve server's IP address.");
                    }
                } else if (command.equalsIgnoreCase("/users")) {
                    if (clientMap.isEmpty()) {
                        System.out.println("no users connected.");
                    } else {
                        int i = 0;
                        for (String name : clientMap.keySet()) {
                            System.out.println(++i + " > " + name);
                        }
                    }
                } else if (command.equalsIgnoreCase("/exit")) {
                    System.out.println("shutting down server...");

                    for (Map.Entry<String, Socket> entry : clientMap.entrySet()) {
                        try {
                            if (entry.getValue() != null && !entry.getValue().isClosed()) {
                                entry.getValue().close();
                            }
                        } catch (IOException e) {
                            System.err.println("error closing client socket (" + entry.getKey() + ") -> " + e.getMessage());
                        }
                    }

                    try {
                        if (serverSocket != null && !serverSocket.isClosed()) {
                            serverSocket.close();
                        }
                    } catch (IOException e) {
                        System.err.println("error while closing server socket -> " + e.getMessage());
                    }

                    System.exit(0);
                } else {
                    System.out.println("unknown command. available commands:");
                    System.out.println("/greeting <new_greeting>  -> changes greeting message");
                    System.out.println("/clientnum                -> shows connected client count");
                    System.out.println("/exit                     -> shuts down the server");
                    System.out.println("/whoami                   -> prints server information");
                    System.out.println("/users                    -> shows users on the server");
                }
            }
        });
        commandThread.setDaemon(true);
        commandThread.start();

        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("server started on port -> " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("new connection -> " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                Thread thread = new Thread(handler, "ClientHandler-" + clientSocket.getInetAddress().getHostAddress());
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("error starting server -> " + e.getMessage());
            return 1;
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.err.println("failed to close server socket -> " + e.getMessage());
            }
        }
    }
}
