package client;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MessageSender {

    private static MessageSender singleInstance = null;
    private        PrintWriter   writer;
    private final  Date          time;
    
    private MessageSender() {
        this.time = new Date();
    }

    public static MessageSender getInstance() {
        if (singleInstance == null) {
            singleInstance = new MessageSender();
        }
        return singleInstance;
    }

    public void initializeMsgSndr(Socket socket) {
        try {
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            }
        } catch (IOException e) {
            System.err.println("failed to initialize writer: " + e.getMessage());
        }
    }

    private String getTimeForMessage() {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        return "[" + formatter.format(time) + "] ";
    }

    public void send(String message) {
        if (writer != null) {
            writer.println(getTimeForMessage() +  message);
        } else {
            System.err.println("writer not initialized, message not sent.");
        }
    }

    public void send(String username, String message) {
        String initMsg = getTimeForMessage() + username + ": ";
        if (writer != null) {
            writer.println(initMsg + message);
        } else {
            System.err.println("writer not initialized, message not sent.");
        }
    }

    public void close() {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }
}
