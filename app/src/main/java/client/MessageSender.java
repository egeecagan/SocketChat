package client;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class MessageSender {

    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";

    private PrintWriter writer;

    public MessageSender(Socket socket) {
        try {
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        } catch (IOException e) {
            System.err.println(RED + "failed to initialize writer: " + e.getMessage() + RESET);
        }
    }

    public void send(String message) {
        if (writer != null) {
            writer.println(message); 
        } else {
            System.err.println(YELLOW + "writer not initialized, message not sent." + RESET);
        }
    }

    public void send(String username, String message) {
        if (writer != null) {
            writer.println(message);
        } else {
            System.err.println(YELLOW + "writer not initialized, message not sent." + RESET);
        }
    }

    public void close() {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }
}
