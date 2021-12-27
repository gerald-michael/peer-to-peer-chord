import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.LinkedHashMap;

import server.Server;

public class Node {
    private InetSocketAddress address;
    private InetSocketAddress pred;
    private InetSocketAddress succ;
    public int id;
    private int predId;
    private int succId;
    private LinkedHashMap fingerTable;
    public static final int MAX_NODES = 100;

    public Node(String ip, int port) throws Exception {
        InetAddress m_ip = null;
        try {
            m_ip = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.address = new InetSocketAddress(m_ip, port);
        this.pred = this.address;
        this.succ = this.address;
        this.id = Node.getHash(this.address.toString());
        this.predId = this.id;
        this.succId = this.id;
        this.fingerTable = new LinkedHashMap<>();
        Server server = new Server(port);
        server.start();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        updateListenToPeers(bufferedReader, server);
    }

    public static void clear() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public void updateListenToPeers(BufferedReader bufferedReader, Server server)
            throws Exception {
        boolean running = true;
        while (running) {
            System.out.println("1. Join Network");
            System.out.println("2. Leave Network");
            System.out.println("3. Upload file");
            System.out.println("4. Downoad file");
            System.out.println("5. Print Finger Table");
            System.out.println("6. Print my Predecessor and Successor");
            System.out.println("7. Clear");
            System.out.println("8. Exit");
            int input = Integer.parseInt(bufferedReader.readLine());
            switch (input) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    break;
                case 6:
                    System.out.println("Successor(" + this.succId + "): " + this.succ.toString().split("/")[1]);
                    System.out.println("Predecessor(" + this.succId + "): " + this.succ.toString().split("/")[1]);
                    break;
                case 7:
                    Node.clear();
                    break;
                case 8:
                    running = false;
                    break;
                default:
                    Node.clear();
                    System.out.println("Incorrect input, please try again");
                    break;
            }
        }
    }

    public static int getHash(String key) throws Exception {
        int result;
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        digest.update(key.getBytes("utf8"));
        BigInteger bigInteger = new BigInteger(1, digest.digest());
        result = bigInteger.mod(BigInteger.valueOf(Node.MAX_NODES)).intValue();
        return result;
    }

    public static void main(String[] args) {
        try {
            // if(args.length < 2){
            //     System.out.println("Please provide port number in arguments");
            //     return;
            // }
            Node mNode = new Node("127.0.0.1", 6000);
        } catch (Exception e) {
        }
    }
}