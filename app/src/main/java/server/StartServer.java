package server;

public class StartServer {

    public static int runServer() {
        Server server = Server.getInstance();
        return server.start();
    }
}