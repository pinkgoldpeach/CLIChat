package chatserver.threads;

import chatserver.Users;
import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by dev on 10/9/15.
 */
public class UDPListenerThread extends Thread {
    private DatagramSocket datagramSocket;
    private Users users;

    public UDPListenerThread(DatagramSocket datagramSocket, Users users){
        this.datagramSocket = datagramSocket;
        this.users = users;
    }

    public void run(){
        DatagramPacket packet;
        while(true){
            try {
                packet = new DatagramPacket(Base64.encode("!list".getBytes()), Base64.encode("!list".getBytes()).length);
                datagramSocket.receive(packet);
                new UDPWorker(packet, users).start();

            } catch (IOException e){
                break;
            }
        }
    }
}