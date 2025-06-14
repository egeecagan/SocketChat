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

    private final Scanner sc;
    private final ReadJson statusReader;

    private static Client singleInstance = null;

    private MessageReciever receiver;
    private MessageSender sender;

    private String username = null;

    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";

    private final String GREET_KW = "haleluyah";

    private String customkw = null;  // i tried to change GREET_KW as the first function called in start but it did not change
    // it was still haleluyah and fyi it was not final at that time and also static so i created another solution whith this one

    private Client(Scanner sc, ReadJson statusReader) {
        this.sc = sc;
        this.statusReader = statusReader;
        this.localIps = new ArrayList<>();


    }

    public static synchronized Client getClient(ReadJson statusReader) {
        return getClient(new Scanner(System.in), statusReader);
    }

    public static synchronized Client getClient(Scanner scanner, ReadJson statusReader) {
        if (singleInstance == null) {
            singleInstance = new Client(scanner, statusReader);
        }
        return singleInstance;
    }

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

    private void getIpAddresses() {
        localIps.clear();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        localIps.add(ip);
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
                System.out.print("what is the port of the server -> ");
                port = sc.nextInt();
                sc.nextLine();

                if (port < 49152 || port > 65535) {
                    System.err.println(YELLOW + "port number must be between 49152 and 65535" + RESET);
                    continue;
                }
                break;
            } catch (InputMismatchException e) {
                System.err.println(YELLOW + "please enter a valid integer for the port" + RESET);
                sc.nextLine();
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
                System.err.println(RED + "no local ip's found" + RESET);
                setStatusCode(10);
                return null;
            }

            for (String ip : localIps) {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, port), 2000);

                    socket.setSoTimeout(2000); 

                    receiver = new MessageReciever(socket);
                    String welcome = receiver.receiveWelcomeMessage();

                    String b = askSpecificGreeting();
                    if (b.equals("y")) {
                        if (welcome != null && welcome.trim().equalsIgnoreCase(customkw.trim())) {
                            System.out.println(GREEN + "valid server found at -> " + ip + RESET);

                            sender = new MessageSender(socket);
                            sender.send("ok");

                            socket.setSoTimeout(0); 

                            return socket;
                        } else {
                            System.err.println(YELLOW + "invalid greeting from -> " + ip + RESET);
                            socket.close();
                        }
                    } else {
                        if (welcome != null && welcome.trim().equalsIgnoreCase(GREET_KW.trim())) {
                            System.out.println(GREEN + "valid server found at -> " + ip + RESET);

                            sender = new MessageSender(socket);
                            sender.send("ok");

                            socket.setSoTimeout(0); 

                            return socket;
                        } else {
                            System.err.println(YELLOW + "invalid greeting from -> " + ip + RESET);
                            socket.close();
                        }
                    }
                    

                } catch (IOException e) {
                    System.err.println(RED + "connection to " + ip + " failed -> " + e.getMessage() + RESET);
                    setStatusCode(11);
                }
            }

            System.out.print(YELLOW + "no valid server found retry with updated ip list? (y, n) -> " + RESET);
            String retry = sc.nextLine().trim().toLowerCase();

            while (!retry.equals("y") && !retry.equals("n")) {
                System.out.print("please enter 'y' or 'n' -> ");
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
            System.err.println(RED + "failed to close socket -> " + e.getMessage() + RESET);
        }
    }

    private String askSpecificGreeting() {
        System.out.println("do you have a special greeting message(y, n)");
        System.out.println("default greet message is 'haleluyah'");
        System.out.println("other answer than 'y' is accepted as 'n'");
        String yn = sc.nextLine().replaceAll("\\s+", "").toLowerCase();
        if (yn.equals("y")) {
            System.out.print("enter the new greeting -> ");
            customkw = sc.nextLine().replaceAll("\\s+", "");
            return "y";
        } else {
            return "n";
        }
    }

    public int start() {

        int port = askPort();

        if (port == -1) return statusCode;

        Socket socket = connectToValidServer(port);
        if (socket == null) return statusCode;

        try {
            System.out.print("what is your user name -> ");
            while (true) {
                username = sc.nextLine().trim().replaceAll("\\s+", "-");

                if (username.isEmpty()) {
                    System.err.println(YELLOW + "username cannot be empty please enter again" + RESET);
                    continue;
                }

                if (username.toLowerCase().startsWith("username:")) {
                    System.err.println(YELLOW + "do not include 'username:' prefix. just enter your name." + RESET);
                    continue;
                }

                sender.send(username);
                System.out.println(YELLOW + "waiting for server response..." + RESET);

                String response = receiver.receiveSingleMessage();

                if (response == null) {
                    System.out.println(response);
                    System.err.println(RED + "no response from server." + RESET);
                    return 13;
                }

                if (response.toLowerCase().contains("username already in use")
                        || response.toLowerCase().contains("invalid format")
                        || response.toLowerCase().contains("username cannot be empty")) {
                    System.err.println(RED + response + RESET);
                    System.out.print("username -> ");
                } else {
                    System.out.println(GREEN + response + RESET);
                    break;
                }
            }

            Thread receiverThread = new Thread(() -> {
                while (!socket.isClosed()) {
                    String msg = receiver.receiveSingleMessage();
                    if (msg != null) {
                        System.out.println(msg);
                    } else {
                        break;
                    }
                }
            });
            receiverThread.setDaemon(true);
            receiverThread.start();

            while (true) {
                String input = sc.nextLine();
                
                if (input.toLowerCase().trim().startsWith("/exit")) {
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
