package client;

import java.io.IOException;
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
import java.util.NoSuchElementException;
import java.util.Scanner;

import status.ReadJson;
import status.Status;

public class Client {
    
    private int statusCode;
    private final List<String> localIps;
    
    private final Scanner  sc;
    private final ReadJson statusReader;

    private static Scanner globalScanner = null;
    private static Client  singleInstance = null;

    private static MessageReciever receiver = null;
    private static MessageSender   sender   = null;

    private static String username = null;

    public static final String RESET   = "\u001B[0m";
    public static final String GREEN   = "\u001B[32m";
    public static final String RED     = "\u001B[31m";
    public static final String YELLOW  = "\u001B[33m";

    private final String GREET_KW = "haleluyah"; // ilerde bunu customizable yapicam

    public int getStatusCode() {
        return statusCode;
    }

    private void setStatusCode(int code) {
        this.statusCode = code;
        Status status = statusReader.getStatus(code);
        if (code == 0) {
            System.out.println(GREEN + status + RESET);
        } else {
            System.err.println(RED + status + RESET);
        }
    }

    private Client(Scanner sc, ReadJson statusReader) {
        this.sc = sc;

        this.statusReader = statusReader;
        this.localIps     = new ArrayList<>();

        sender   = MessageSender.getInstance();
        receiver = MessageReciever.getInstance();
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
        localIps.clear(); // onceden kalanlari temizlemek icin ekledik cunku ilk server aramasi basarisiz ise ikinci aramada temizlik lazim
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

    private Socket connectToValidServer(int port) {
        while (true) {
            getIpAddresses();

            if (localIps.isEmpty()) {
                System.err.println(RED + "no local IPs found." + RESET);
                setStatusCode(10);
                return null;
            }

            for (String ip : localIps) {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, port), 1000);
                    socket.setSoTimeout(1000);

                    receiver.initializeMsgRcv(socket);
                    String welcome = receiver.receiveWelcomeMessage();

                    if (welcome != null && welcome.startsWith(GREET_KW)) {
                        System.out.println(GREEN + "valid server found at: " + ip + RESET);
                        sender.initializeMsgSndr(socket);
                        return socket;
                    } else {
                        System.err.println(YELLOW + "invalid greeting from " + ip + ": " + welcome + RESET);
                        socket.close();
                    }

                } catch (IOException e) {
                    System.err.println(RED + "connection to " + ip + " failed: " + e.getMessage() + RESET);
                    setStatusCode(11);
                }
            }

            System.out.print("no valid server found retry with updated IP list? (y/n): ");
            String retry = sc.nextLine().trim().toLowerCase();

            while (!retry.equals("y") && !retry.equals("n")) {
                System.out.print("please enter 'y' or 'n': ");
                retry = sc.nextLine().trim().toLowerCase();
            }

            if (retry.equals("n")) {
                break;
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
    
    public int start() {

        System.out.print("what is your user name: ");
        username = sc.nextLine().trim();

        while (username.isEmpty()) {
            System.err.println("username cannot be empty. please enter again.");
            System.out.print("what is your user name: ");
            username = sc.nextLine().trim().replaceAll("\\s+", "-");
        }

        System.out.println();
        System.out.printf("hello %s\n", username);

        int port = askPort();
        if (port == -1)
            return statusCode;

        Socket socket = connectToValidServer(port);
        if (socket == null)
            return statusCode;

        try {
            sender.send("username:" + username);

            Thread receiverThread = new Thread(() -> {
                while (!socket.isClosed()) {
                    String msg = receiver.receiveSingleMessage();
                    if (msg != null) {
                        System.out.println("\n" + msg);
                        System.out.print(">>> ");
                    }
                }
            });
            receiverThread.start();

            while (true) {
                System.out.print(">>> ");
                String input = sc.nextLine();

                if (input.toLowerCase().trim().startsWith("/exit")) {
                    sender.send(username, "!!disconnected");
                    break;
                }

                sender.send(username, input);
            }

        } catch (Exception e) {
            setStatusCode(11);
        } finally {
            closeSocket(socket);
            System.out.println(GREEN + "disconnected from chat, bye." + RESET);
        }

        return statusCode;
    }


}
