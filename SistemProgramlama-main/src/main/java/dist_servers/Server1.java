package dist_servers;

import java.io.IOException;

public class Server1 {
    public static void main(String[] args) {
        try {
           ServerBase server1 = new ServerBase(1);
           server1.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
