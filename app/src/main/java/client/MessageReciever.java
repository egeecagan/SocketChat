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
            if (this.reader == null)
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.println("failed to initialize reader " + e.getMessage());
        }
    }
    
    public String receiveWelcomeMessage() {
        StringBuilder message = new StringBuilder();
        try {
            for (int i = 0; i < 3; i++) {
                String line = reader.readLine();
                if (line != null) {
                    message.append(line).append("\n");
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("error receiving welcome message: " + e.getMessage());
        }
        return message.toString().trim();
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
