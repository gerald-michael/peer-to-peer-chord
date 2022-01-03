import java.io.BufferedReader;
import java.io.InputStreamReader;
import server.Server;

public class Node {
    public Node(String ip, int port) throws Exception {
        Server server = new Server(ip, port);
        server.start();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        server.updateListenToPeers(bufferedReader, server);
    }

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.out.println("Please provide ip and port number in arguments");
                return;
            }
            Node mNode = new Node(args[0], Integer.parseInt(args[1]));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
