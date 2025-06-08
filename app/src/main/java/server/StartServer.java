package server;

public class StartServer {

    public static int runServer() {
        Server server = Server.getInstance();
        return server.start(); // Başlat ve sonucunu döndür (0: başarı, 1: hata)
    }
}