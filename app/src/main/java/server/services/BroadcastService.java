package server.services;

import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Map;

import server.Server;

public class BroadcastService {

    public static void broadcast(String message, Socket senderSocket, String senderName) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        String currentTime = formatter.format(new java.util.Date());

        String formattedMessage = "[" + currentTime + "] " + senderName + ": " + message;

        for (Map.Entry<String, Socket> entry : Server.clientMap.entrySet()) {
            Socket socket = entry.getValue();

            try {
                if (!socket.isClosed()) {
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                    writer.println(formattedMessage); // 💡 herkese aynı mesaj
                }
            } catch (Exception e) {
                System.err.println("broadcast error: " + e.getMessage());
            }
        }
    }
}
