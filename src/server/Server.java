package server;

import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
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
import java.util.Scanner;
import java.util.Set;

import org.json.JSONArray;
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

    public void downloadFile(String filename) throws Exception {

        System.out.println("Downloading file");
        int fileId = Server.getHash(filename);

        InetSocketAddress receiveAddress = this.getSuccessor(this.succ, fileId);
        JSONObject obj = new JSONObject();
        obj.put("key", 1);
        obj.put("filename", filename);
        obj.put("choice", 0);
        Socket socket = new Socket(receiveAddress.getHostName(), receiveAddress.getPort());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeUTF(obj.toString());
        DataInputStream in = new DataInputStream(socket.getInputStream());
        JSONObject jsonObject = new JSONObject(in.readUTF());
        int result = jsonObject.getInt("result");
        if (result == 1) {
            FileWriter fileWriter = new FileWriter(filename);
            fileWriter.write(jsonObject.getString("file"));
            fileWriter.close();
            System.out.println("File downloaded successfully");

        } else {
            System.out.println("File not found");
        }
    }

    public void uploadFile(String filename, InetSocketAddress recieveAddress, boolean replicate) {
        System.out.println("Uploading file");
        String message = new String();
        JSONObject obj = new JSONObject();
        if (replicate) {
            obj.put("replicate", 1);
        } else {
            obj.put("replicate", -1);
        }
        obj.put("choice", 1);
        try {
            File file = new File(filename);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                message += scanner.nextLine();
                message += "\n";
            }
            scanner.close();
            obj.put("message", message);
            obj.put("filename", filename);
            Socket socket = new Socket(recieveAddress.getHostName(), recieveAddress.getPort());
            obj.put("key", 1);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(obj.toString());
        } catch (Exception e) {
            e.printStackTrace();
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
                    this.leaveNetwork();
                    break;
                case 3:
                    System.out.println("File name: ");
                    String filename = bufferedReader.readLine();
                    int fileId = Server.getHash(filename);
                    InetSocketAddress receiverAddress = this.getSuccessor(this.succ, fileId);
                    this.uploadFile(filename, receiverAddress, true);
                    break;
                case 4:
                    System.out.println("File name: ");
                    String downloadFilename = bufferedReader.readLine();
                    this.downloadFile(downloadFilename);
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

    public void leaveNetwork() throws Exception {
        // inform successor to update its predeccessor
        Socket socket = new Socket(this.succ.getHostName(), this.succ.getPort());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", 4);
        jsonObject.put("op", 0);
        jsonObject.put("address", this.pred.toString().split("/")[1]);
        out.writeUTF(jsonObject.toString());
        out.close();
        socket.close();
        // inform predecessor to update its successor
        Socket socket2 = new Socket(this.pred.getHostName(), this.pred.getPort());
        DataOutputStream out2 = new DataOutputStream(socket2.getOutputStream());
        JSONObject jsonObject2 = new JSONObject();
        jsonObject2.put("key", 4);
        jsonObject2.put("op", 1);
        jsonObject2.put("address", this.succ.toString().split("/")[1]);
        out2.writeUTF(jsonObject2.toString());
        out2.close();
        socket2.close();
        // replicate files to successor
        this.files.forEach((filename) -> {
            try {
                Socket socket3 = new Socket(this.succ.getHostName(), this.succ.getPort());
                DataOutputStream out3 = new DataOutputStream(socket3.getOutputStream());
                JSONObject jsonObject3 = new JSONObject();
                jsonObject3.put("key", 1);
                String message = "";
                File file = new File(filename);
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextLine()) {
                    message += scanner.nextLine();
                    message += "\n";
                }
                scanner.close();
                jsonObject3.put("message", message);
                jsonObject3.put("filename", filename);
                jsonObject3.put("replicate", -1);
                out3.writeUTF(jsonObject3.toString());
                out3.close();
                socket3.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        // tell others to update there finger tables
        this.updateOtherFingerTables();
        // changing predecessor and successor to myself
        this.pred = this.address;
        this.predId = this.id;
        this.succ = this.address;
        this.succId = this.id;
        // clear finger table
        this.fingerTable.clear();
        System.out.println(this.address.toString().split("/")[1] + " has left the network");
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
