package server;
import java.net.InetSocketAddress;

public class Peer {
    private int id;
    private InetSocketAddress address;

    public Peer(int id, InetSocketAddress address) {
        this.setId(id);
        this.setAddress(address);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }
}
