package client.threads;

import channel.IChannel;
import client.MessageDelivery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by dev on 10/9/15.
 */
public class UDPListenerThread extends Thread {
    private IChannel udpChannel;
    private MessageDelivery msgDel;

    public UDPListenerThread(IChannel udpChannel, MessageDelivery msgDel) {
        this.udpChannel = udpChannel;
        this.msgDel = msgDel;
    }

    public void run(){
        while(true) {
            try {
                String request = new String(udpChannel.receive());
                request = request.replaceAll("]", "").replaceAll(", ", "\n").replaceAll("\\[", "");
                request = request.substring(0, request.indexOf("\0"));
                msgDel.put(request);

            } catch (IOException e) {
                break;
            }
        }
    }
}
