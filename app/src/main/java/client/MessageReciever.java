package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MessageReciever {

    private static MessageReciever singleInstance = null;
    private BufferedReader reader;

    private MessageReciever() {
    }

    public static MessageReciever getInstance() {
        if (singleInstance == null) {
            singleInstance = new MessageReciever();
        }
        return singleInstance;
    }

    public void initializeMsgRcv(Socket socket) {
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.println("failed to initialize reader: " + e.getMessage());
        }
    }

    public String receiveWelcomeMessage() {
        try {
            if (reader == null) {
                System.err.println("reader is null in receiveWelcomeMessage()");
                return null;
            }
            return reader.readLine();
        } catch (IOException e) {
            System.err.println("error receiving welcome message: " + e.getMessage());
            return null;
        }
    }

    public String receiveSingleMessage() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            System.err.println("error receiving message: " + e.getMessage());
            return null;
        }
    }
}

