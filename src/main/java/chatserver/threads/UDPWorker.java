package chatserver.threads;


import channel.Base64Channel;
import channel.IChannel;
import channel.UDPChannel;
import chatserver.Users;
import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.net.DatagramPacket;

import java.util.*;

/**
 * Created by dev on 10/9/15.
 */
public class UDPWorker extends Thread {
    private DatagramPacket packet;
    private Users users;
    private String data;


    public UDPWorker(DatagramPacket packet, Users users){
        this.packet = packet;
        this.users = users;
    }

    public void run(){
        data = new String(Base64.decode(packet.getData()));

        if ("!list".equals(data)) {
            try {
                IChannel udpChannel = new Base64Channel(new UDPChannel(packet.getAddress().getHostName(), packet.getPort()));
                SortedSet<String> user = new TreeSet<>();
                for(String name : users.getOnlineUsers().values()){
                    user.add(name);
                }
                udpChannel.send(user.toString().getBytes());
            } catch (IOException e) {}
        }
    }
}
