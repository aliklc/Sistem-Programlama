package dist_servers;


import java.io.IOException;

public class Server3 {
    public static void main(String[] args) {
        try {
            ServerBase server1 = new ServerBase(3);
            server1.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
