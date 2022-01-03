package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.json.JSONObject;

public class ServerThread extends Thread {
    private Server server;
    private Socket socket;
    private PrintWriter printWriter;

    public ServerThread(Socket socket, Server server) {
        this.server = server;
        this.socket = socket;
    }

    public void run() {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            JSONObject res = new JSONObject(in.readUTF());
            int key = res.getInt("key");
            switch (key) {
                case 0:
                    // join network
                    String recvIpPort[] = res.getString("address").split(":");
                    this.joinNode(new InetSocketAddress(recvIpPort[0], Integer.parseInt(recvIpPort[1])));
                    break;
                case 1:
                    // upload or download file
                    System.out.println("Upload/Download request recevied");
                    int choice = res.getInt("choice");
                    String filename = res.getString("filename");
                    int replicate = 0;

                    String file = null;
                    if (choice == 1) {
                        file = res.getString("message");
                        replicate = res.getInt("replicate");
                    }
                    break;
                case 2:
                    // stabilize
                    break;
                case 3:
                    // lookup request
                    this.lookupId(res.getInt("keyId"));
                    break;
                case 4:
                    // update predecessor or successor
                    int op = res.getInt("op");
                    String adr[] = res.getString("address").split(":");
                    if (op == 1) {
                        server.updateSuccessor(new InetSocketAddress(adr[0], Integer.parseInt(adr[1])));
                    } else {
                        server.updatePredecessor(new InetSocketAddress(adr[0], Integer.parseInt(adr[1])));
                    }
                    break;
                case 5:
                    // update finger table request
                    server.updateFingerTable();
                    DataOutputStream out2 = new DataOutputStream(socket.getOutputStream());
                    JSONObject jsonObject2 = new JSONObject();
                    jsonObject2.put("succ", server.getSucc().toString().split("/")[1]);
                    out2.writeUTF(jsonObject2.toString());
                    break;
                case 6:
                    // retrieve files
                    break;
                case default:
                    System.out.println("Invalid connection request");
                    break;
            }
            // this.printWriter = new PrintWriter(socket.getOutputStream(), true);
            // while (true)
            // server.sendMessage(bufferedReader.readLine());
        } catch (Exception e) {
            server.getServerThreadThreads().remove(this);
        }
    }

    public void lookupId(int keyId) throws Exception {
        JSONObject jsonObject = new JSONObject();
        if (keyId == server.getServerId()) {
            jsonObject.put("result", server.getAddress().toString().split("/")[1]);
            jsonObject.put("status", 0);
        } else if (server.getSuccId() == server.getServerId()) {
            jsonObject.put("result", server.getAddress().toString().split("/")[1]);
            jsonObject.put("status", 0);
        } else if (server.getServerId() > keyId) {
            if (server.getPredId() < keyId) {
                jsonObject.put("result", server.getAddress().toString().split("/")[1]);
                jsonObject.put("status", 0);
            } else if (server.getPredId() > server.getId()) {
                jsonObject.put("result", server.getAddress().toString().split("/")[1]);
                jsonObject.put("status", 0);
            } else {
                jsonObject.put("result", server.getPred().toString().split("/")[1]);
                jsonObject.put("status", 1);
            }
        } else {
            if (server.getServerId() > server.getSuccId()) {
                jsonObject.put("result", server.getSucc().toString().split("/")[1]);
                jsonObject.put("status", 0);
            } else {
                InetSocketAddress addr = server.getSucc();
                jsonObject.put("result", addr.toString().split("/")[1]);
                jsonObject.put("status", 1);
            }
        }
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeUTF(jsonObject.toString());
    }

    public void joinNode(InetSocketAddress address) throws Exception {
        int peerID = Server.getHash(address.toString().split("/")[1]);
        InetSocketAddress oldPred = this.server.getPred();

        // update predecessor
        this.server.setPred(address);
        this.server.setPredId(peerID);
        // sending new peer's predecesor back to it
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("pred", oldPred.toString().split("/")[1]);
        out.writeUTF(jsonObject.toString());
        Thread.sleep(100);
        // update finger table
        server.updateFingerTable();
        // ask other peers to update finger table
        server.updateOtherFingerTables();
    }

    public PrintWriter getPrintWriter() {
        return printWriter;
    }
}
