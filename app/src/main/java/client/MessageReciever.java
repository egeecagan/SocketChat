package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MessageReciever {

    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";

    private BufferedReader reader;

    public MessageReciever(Socket socket) {
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.println(RED + "failed to initialize reader: " + e.getMessage() + RESET);
        }
    }

    public String receiveWelcomeMessage() {
        try {
            if (reader == null) {
                System.err.println(RED + "reader is null in receiveWelcomeMessage()" + RESET);
                return null;
            }
            return reader.readLine();
        } catch (IOException e) {
            System.err.println(RED + "error receiving welcome message: " + e.getMessage() + RESET);
            return null;
        }
    }

    public String receiveSingleMessage() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            System.err.println(RED + "error receiving message: " + e.getMessage() + RESET);
            return null;
        }
    }
}
