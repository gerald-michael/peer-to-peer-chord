package server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

public class Server extends Thread {
    private InetSocketAddress address;
    private InetSocketAddress pred;
    private InetSocketAddress succ;
    private ArrayList<String> files;
    private int id;
    private int predId;
    private int succId;
    private LinkedHashMap<Integer, Peer> fingerTable;
    public static final int MAX_BIT = 10;
    public static final int MAX_NODES = (int) Math.pow(2, MAX_BIT);

    private ServerSocket serverSocket;
    private Set<ServerThread> serverThreads = new HashSet<ServerThread>();

    public Server(String ip, int port) throws Exception {
        InetAddress m_ip = null;
        try {
            m_ip = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.files = new ArrayList<String>();
        this.address = new InetSocketAddress(m_ip, port);
        this.setPred(this.address);
        this.succ = this.address;
        this.id = Server.getHash(this.address.toString().split("/")[1]);// 127.0.0.1:5000
        this.setPredId(this.id);
        this.succId = this.id;
        this.fingerTable = new LinkedHashMap<Integer, Peer>();
        serverSocket = new ServerSocket(port);
    }

    public boolean checkFileExists(String filename) {
        return this.files.contains(filename);
    }

    public void addFile(String filename) {
        this.files.add(filename);
    }

    public ArrayList<String> getFiles() {
        return this.files;
    }

    public InetSocketAddress getPred() {
        return pred;
    }

    public InetSocketAddress getSucc() {
        return succ;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setPred(InetSocketAddress pred) {
        this.pred = pred;
    }

    public int getPredId() {
        return predId;
    }

    public int getSuccId() {
        return succId;
    }

    public int getServerId() {
        return id;
    }

    public void setPredId(int predId) {
        this.predId = predId;
    }

    public static int getHash(String key) throws Exception {
        int result;
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        digest.update(key.getBytes("utf8"));
        BigInteger bigInteger = new BigInteger(1, digest.digest());
        result = bigInteger.mod(BigInteger.valueOf(Server.MAX_NODES)).intValue();
        return result;
    }

    public void updateFingerTable() throws Exception {
        for (int i = 0; i < Server.MAX_BIT; i++) {
            int entryId = (this.id + (int) (Math.pow(2, i))) % Server.MAX_NODES;
            if (this.succ == this.address) {
                this.fingerTable.put(entryId, new Peer(this.id, this.address));
                continue;
            }
            InetSocketAddress receivedAddress = this.getSuccessor(this.succ, entryId);
            int recieverId = Server.getHash(receivedAddress.toString().split("/")[1]);
            this.fingerTable.put(entryId, new Peer(recieverId, receivedAddress));
        }
    }

    public void updateSuccessor(InetSocketAddress succAddress) throws Exception {
        this.succ = succAddress;
        this.succId = Server.getHash(succAddress.toString().split("/")[1]);
    }

    public void updatePredecessor(InetSocketAddress address) throws Exception {
        this.pred = address;
        this.predId = Server.getHash(address.toString().split("/")[1]);
    }

    public void printFingerTable() {
        System.out.println("Printing finger Table");
        for (Map.Entry<Integer, Peer> element : this.fingerTable.entrySet()) {
            System.out.println("ID: " + element.getKey() + "\t value: (" + element.getValue().getId() + ") "
                    + element.getValue().getAddress().toString().split("/")[1]);
        }
    }



    public InetSocketAddress getSuccessor(InetSocketAddress address, int keyId) {
        Socket socket = null;
        String ad[];
        while (true) {
            try {
                socket = new Socket(address.getHostName(), address.getPort());
                JSONObject obj = new JSONObject();
                obj.put("key", 3);
                obj.put("keyId", keyId);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF(obj.toString());
                DataInputStream in = new DataInputStream(socket.getInputStream());
                JSONObject res = new JSONObject(in.readUTF());
                ad = res.getString("result").split(":");
                address = new InetSocketAddress(ad[0], Integer.parseInt(ad[1]));
                if (res.getInt("status") == 0) {
                    break;
                }
            } catch (Exception e) {
                System.out.println("Something went wrong when getting successor");
                return null;
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("close:" + e.getMessage());
                }
            }
        }
        return new InetSocketAddress(ad[0], Integer.parseInt(ad[1]));
    }

    public void sendJoinRequest(String ip, int port) {
        try {
            InetSocketAddress address = this.getSuccessor(new InetSocketAddress(ip, port), this.id);
            Socket socket = new Socket(address.getHostName(), address.getPort());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("key", 0);
            jsonObject.put("address", this.address.toString().split("/")[1]);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(jsonObject.toString());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            JSONObject res = new JSONObject(in.readUTF());
            String predIPPort[] = res.getString("pred").split(":");
            // update predecessor and successor
            this.pred = new InetSocketAddress(predIPPort[0],
                    Integer.parseInt(predIPPort[1]));
            this.predId = Server.getHash(this.pred.toString().split("/")[1]);
            this.succ = address;
            this.succId = Server.getHash(address.toString().split("/")[1]);

            // tell predecessor that am his successor
            Socket socket2 = new Socket(this.pred.getHostName(), this.pred.getPort());
            JSONObject jsonObject2 = new JSONObject();
            jsonObject2.put("key", 4);
            jsonObject2.put("op", 1);
            jsonObject2.put("address", this.address.toString().split("/")[1]);
            DataOutputStream out2 = new DataOutputStream(socket2.getOutputStream());
            out2.writeUTF(jsonObject2.toString());
            socket.close();
            socket2.close();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clear() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public void updateListenToPeers(BufferedReader bufferedReader, Server server)
            throws Exception {
        boolean running = true;
        while (running) {
            System.out.println("Peer ID: (" + this.id + ")");
            System.out.println("1. Join Network");
            System.out.println("2. Leave Network");
            System.out.println("3. Upload file");
            System.out.println("4. Download file");
            System.out.println("5. Print Finger Table");
            System.out.println("6. Print my Predecessor and Successor");
            System.out.println("7. Print my files");
            System.out.println("8. Clear");
            System.out.println("9. Exit");
            int input = Integer.parseInt(bufferedReader.readLine());
            switch (input) {
                case 1:
                    System.out.println("Enter IP: ");
                    String ip = bufferedReader.readLine();
                    System.out.println("Enter Port: ");
                    int port = Integer.parseInt(bufferedReader.readLine());
                    this.sendJoinRequest(ip, port);
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    this.printFingerTable();
                    break;
                case 6:
                    System.out.println("Successor(" + this.succId + "): " + this.succ.toString().split("/")[1]);
                    System.out.println("Predecessor(" + this.predId + "): " + this.pred.toString().split("/")[1]);
                    break;
                case 7:
                    for (int i = 0; i < this.files.size(); i++) {
                        System.out.println("(" + i + ") " + this.files.get(i));
                    }
                    break;
                case 8:
                    Server.clear();
                    break;
                case 9:
                    running = false;
                    break;
                default:
                    Server.clear();
                    System.out.println("Incorrect input, please try again");
                    break;
            }
        }
    }

    public void updateOtherFingerTables() {
        InetSocketAddress successor = this.succ;
        while (true) {
            if (successor == this.address) {
                break;
            }
            try {
                Socket socket = new Socket(successor.getHostName(), successor.getPort());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("key", 5);
                out.writeUTF(jsonObject.toString());
                DataInputStream in = new DataInputStream(socket.getInputStream());
                JSONObject res = new JSONObject(in.readUTF());
                String addr[] = res.getString("succ").split(":");
                successor = new InetSocketAddress(addr[0], Integer.parseInt(addr[1]));
                socket.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void run() {
        // listen for connections
        try {
            while (true) {
                ServerThread serverThread = new ServerThread(serverSocket.accept(), this);
                serverThreads.add(serverThread);
                serverThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Set<ServerThread> getServerThreadThreads() {
        return serverThreads;
    }
}
