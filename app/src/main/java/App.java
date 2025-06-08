
import java.io.IOException;
import java.io.InputStream;

import client.StartClient;
import server.StartServer;
import status.ReadJson;
import status.Status;

public class App {

    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";

    public static void main(String[] args) {

        ReadJson reader = null;

        try (InputStream in = App.class.getClassLoader().getResourceAsStream("status_codes.json")) {
            if (in == null) {
                System.err.println(RED + "status_codes.json not found!" + RESET);
                System.exit(1);
            }
            reader = new ReadJson(in);
        } catch (IOException e) {
            System.err.println(RED + "an error occurred while reading status_codes.json: " + e.getMessage() + RESET);
            System.exit(1);
        }

        if (args.length != 1) {
            throw new IllegalArgumentException(YELLOW + "exactly one argument is required: [client|server]" + RESET);
        }

        switch (args[0].toLowerCase().trim()) {

            case "client" -> {
                int clientStatusAsInt = StartClient.runClient();
                if(clientStatusAsInt == 0) {

                } else {
                    
                    Status clientStatusObj = reader.getStatus(clientStatusAsInt);
                    String message = clientStatusObj.getMessage();
                    System.err.println(RED + message + RESET); 
                    
                }
            }

            case "server" -> {
                int serverStatusAsInt = StartServer.runServer();
                if(serverStatusAsInt == 0) {
                    System.out.println(GREEN + "server has started" + RESET);
                } else {
                    Status serverStatusObj = reader.getStatus(serverStatusAsInt);
                    String message = serverStatusObj.getMessage();
                    System.err.println(RED + message + RESET);
                }
            }

            default -> {
                System.err.println(RED + "usage: java -jar all-app.jar [client|server]" + RESET);
                throw new IllegalArgumentException(YELLOW + "unknown argument: " + args[0] + RESET);
            }
        }
    }
}
