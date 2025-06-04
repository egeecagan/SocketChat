package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class Client {
    
    private int statusCode;
    private static Client singleInstance = null;
    private final List<String> localIps;
    private final Scanner sc;

    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";

    private Client(Scanner scanner) {
        this.sc = scanner;
        this.localIps = new ArrayList<>();
    }

    public static synchronized Client getClient() {
        return getClient(new Scanner(System.in));
    }

    public static synchronized Client getClient(Scanner scanner) {
        if (singleInstance == null) {
            singleInstance = new Client(scanner);
        }
        return singleInstance;
    }

    private void getIpAddresses() {
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
            System.err.println(RED + "failed to retrieve network interfaces: " + e.getMessage() + RESET);
        }   
    }

    //returns the port number
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
                sc.nextLine();
            }
        }

        return port;
    }


    private String findServer(int port) {
        for (String ip : localIps) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, port), 1000); 

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
                );

                socket.setSoTimeout(1000); 

                String response = reader.readLine();

                if ("haleluyah".equals(response)) {
                    System.out.println(GREEN + "valid server found at: " + ip + RESET);
                    return ip;
                } else {
                    System.err.println(RED + "unexpected server response from: " + ip + " -> " + response + RESET);
                }

            } catch (Exception e) {
                
            }
        }

        System.err.println(RED + "no valid server found." + RESET);
        return null;
    }

    private  Socket connect(String ip, int port) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(ip, port), 1000);
        socket.setSoTimeout(1000);
        return socket;
    }

    


    public int start() {
        
        return statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
