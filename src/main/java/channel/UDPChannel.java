package channel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by dev on 10/22/15.
 */
public class UDPChannel implements IChannel {
    DatagramSocket datagramSocket;
    String host;
    int port;

    public UDPChannel(String host, int port) throws IOException{
        datagramSocket = new DatagramSocket();
        this.host = host;
        this.port = port;
    }

    @Override
    public void send(byte[] data) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(host), port);
        datagramSocket.send(packet);
    }

    @Override
    public byte[] receive() throws IOException {
        byte[] buffer = new byte[4096];
        DatagramPacket packet = new DatagramPacket(buffer, 4096);
        datagramSocket.receive(packet);
        return packet.getData();
    }

    public void close() throws IOException{
        datagramSocket.close();
    }

    @Override
    public boolean isClosed(){
        if(datagramSocket == null)
            return true;
        return false;
    }
}
