package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.InputMismatchException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import status.ReadJson;
import status.Status;

public class Client {
    
    private int statusCode;
    private final List<String> localIps;
    private final Scanner sc;
    private final ReadJson statusReader;

    private static Scanner globalScanner = null;
    private static Client singleInstance = null;

    private static String username = null;

    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";


    private final static String SERVER_RESPONSE = "haleluyah";

    private void setStatusCode(int code) {
        this.statusCode = code;
        Status status = statusReader.getStatus(code);
        if (code == 0) {
            System.out.println(GREEN + status + RESET);
        } else {
            System.err.println(RED + status + RESET);
        }
    }

    public int getStatusCode() {
        return statusCode;
    }

    private Client(Scanner sc, ReadJson statusReader) {
        this.sc = sc;
        this.statusReader = statusReader;
        this.localIps = new ArrayList<>();
    }

    public static synchronized Client getClient(ReadJson statusReader) {
        if (globalScanner == null) {
            globalScanner = new Scanner(System.in);
        } 
        return getClient(globalScanner, statusReader);
    }

    public static synchronized Client getClient(Scanner scanner, ReadJson statusReader) {
        if (singleInstance == null) {
            singleInstance = new Client(scanner, statusReader);
        }
        return singleInstance;
    }

    private void getIpAddresses() {
        localIps.clear();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        localIps.add(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            setStatusCode(10); 
        }
    }

    private int askPort() {
        int port = -1;
        while (true) {
            try {
                System.out.print("what is the port of the server? ");
                port = sc.nextInt();

                if (port < 0 || port > 65535) {
                    System.err.println(YELLOW + "port number must be between 0 and 65535." + RESET);
                    continue;
                }
                break;
            } catch (InputMismatchException e) {
                System.err.println(YELLOW + "please enter a valid integer for the port." + RESET);
                sc.nextLine(); // clear the buffer
            } catch (NoSuchElementException | IllegalStateException e) {
                setStatusCode(14); 
                return -1;
            }
        }
        return port;
    }

    private String findServer(int port) {
        for (String ip : localIps) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, port), 1000); 
                socket.setSoTimeout(1000); 
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
                );
                String response = reader.readLine();
                if (SERVER_RESPONSE.equals(response)) {
                    System.out.println(GREEN + "valid server found at: " + ip + RESET);
                    return ip;
                } else {
                    System.err.println(YELLOW + "unexpected server response from: " + ip + " -> " + response + RESET);
                    setStatusCode(13); 
                }
            } catch (IOException e) {
                setStatusCode(11);
            }
        }
        setStatusCode(12); 
        return null;
    }

    private void closeSocket(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println(RED + "failed to close socket: " + e.getMessage() + RESET);
        }
    }















    private Socket connect(String ip, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 1000);
            socket.setSoTimeout(1000);
            return socket;
        } catch (IOException e) {
            setStatusCode(11); 
            return null;
        }
    }


    private int recieveMessage(Socket server) {
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(server.getInputStream())
            );
            String response = reader.readLine();
            if (response != null) {

                // response starts with the "username: "
                System.out.println("\n" + response);
            }
            return 0;
        } catch (SocketTimeoutException e) {
            return 8;
        } catch (IOException e) {
            return 11;
        }
    }
    
    public int start() {

        System.out.print("what is your user name: ");
        username = sc.nextLine().trim();

        while (username.isEmpty()) {
            System.err.println("username cannot be empty. please enter again.");
            System.out.print("what is your user name: ");
            username = sc.nextLine().trim().replaceAll("\\s+", "");
        }

        System.out.println();
        System.out.printf("hello %s\n", username);

        getIpAddresses();
        if (localIps.isEmpty()) 
            return statusCode;
        int port = askPort();
        if (port == -1) 
            return statusCode;
        String serverIp = findServer(port);
        if (serverIp == null) 
            return statusCode;
        Socket socket = connect(serverIp, port);
        if (socket == null) 
            return statusCode;


        Thread receiver = new Thread(() -> {
            while (!socket.isClosed()) {
                recieveMessage(socket);
            }
        });
        receiver.start();

        try {
            sc.nextLine();
            while (true) {
                System.out.print(">>> ");
                String message = sc.nextLine();
                if (message.toLowerCase().trim().startsWith("/exit")) {
                    break;
                }
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println(message);
            }
        } catch (IOException e) {
            setStatusCode(11);
        } finally {
            closeSocket(socket);
            System.out.println(GREEN + "disconnected from chat bye." + RESET);
        }
        return statusCode;
    }
}
