package client;

import java.io.InputStream;

import status.ReadJson;

public class StartClient {

    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";

    @SuppressWarnings("UseSpecificCatch")
    public static int runClient() {
        try {
            InputStream in = StartClient.class.getClassLoader().getResourceAsStream("status_codes.json");

            if (in == null) {
                System.err.println(RED + "couldn't find status_codes.json in resources." + RESET);
                return 1;
            }

            ReadJson statusReader = new ReadJson(in);
            Client user = Client.getClient(statusReader);
            return user.start();

        } catch (Exception e) {
            System.err.println(RED + "unexpected error while running client: " + e.getMessage() + RESET);
            return 1; 
        }
    }
}
