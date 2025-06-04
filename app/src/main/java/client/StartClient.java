package client;

public class StartClient {



    public static int runClient() {
    
        Client user = Client.getClient();
        user.start();


        return 0;
    }
}
