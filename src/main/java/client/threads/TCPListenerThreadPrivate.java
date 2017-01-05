package client.threads;

import channel.Base64Channel;
import channel.IChannel;
import channel.TCPChannel;
import org.bouncycastle.util.encoders.Base64;
import util.Config;
import util.Keys;
import java.lang.String;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import java.security.Key;
import java.security.MessageDigest;
import javax.crypto.Mac;

/**
 * Created by dev on 10/9/15.
 */
public class TCPListenerThreadPrivate extends Thread {
    ServerSocket serverSocket;
    Config config;

    public TCPListenerThreadPrivate(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        config = new Config("client");
    }

    public void run(){
        while(true){
            try {
                IChannel tcpChannel = new Base64Channel(new TCPChannel(serverSocket.accept()));
                String stringMsg = new String(tcpChannel.receive());

                String[] streamparts = stringMsg.split(" ",3);
                    String message = streamparts[2];
                    String command = streamparts[1];
                    String receivedHMAC = streamparts[0];

                    File file = new File(config.getString("hmac.key"));
                    Key secretKey =  Keys.readSecretKey(file);
                    // make sure to use the right ALGORITHM for what you want to do (see text)
                    Mac hMac = Mac.getInstance("HmacSHA256");
                    hMac.init(secretKey);

                // computedHash is the HMAC of the received plaintext
                    byte[] computeHash = Base64.encode(hMac.doFinal(("!msg "+message).getBytes()));
                    boolean validHash = MessageDigest.isEqual(receivedHMAC.getBytes(), computeHash);
                System.out.println(message);
                if (validHash) {
                    byte[] tamperedHash = Base64.encode(hMac.doFinal(("!ack "+message).getBytes()));
                    tcpChannel.send((new String(tamperedHash) + " !ack "+message).getBytes());
                } else {
                    byte[] tamperedHash = Base64.encode(hMac.doFinal(("!tampered "+message).getBytes()));
                    tcpChannel.send((new String(tamperedHash) + " !tampered "+message).getBytes());
                }

            } catch (Exception e) {
                break;
            }
        }
    }
}
