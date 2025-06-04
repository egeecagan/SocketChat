
import client.StartClient;
import server.StartServer;

public class App {
    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("exactly one argument is required: [client|server]");
        }

        switch (args[0].toLowerCase().trim()) {
            case "client" -> {
                if(StartClient.runClient() == 0) {
                    System.out.println("client has started");
                } else {
// add err inf
                }
            }
            case "server" -> {
                if(StartServer.runServer() == 0) {
                    System.out.println("server has started");
                } else {
// add err inf
                }
            }
            default -> {
                System.err.println("usage: java App [client|server]");
                throw new IllegalArgumentException("unknown argument: " + args[0]);
            }
        }
    }
}
