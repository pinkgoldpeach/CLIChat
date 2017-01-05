package client.threads;

import client.MessageDelivery;
import security.AES_Channel;
import java.io.IOException;

/**
 * Created by dev on 10/9/15.
 */
public class TCPListenerThread extends Thread {
    private MessageDelivery msgDel;
    private AES_Channel aes_channel;


    public TCPListenerThread(AES_Channel aes_channel, MessageDelivery msgDel) {
        this.aes_channel = aes_channel;
        this.msgDel = msgDel;
    }

    public void run(){
        while(true) {
            try {
                String msg = new String(aes_channel.receive());
                if (msg.regionMatches(0, "message", 0, 7)) {
                    msg = msg.substring(8);
                    System.out.println(msg);
                    msgDel.putLastMsg(msg);
                } else if("Successfully logged out.".equals(msg)){
                    msgDel.put(msg);
                    aes_channel.close();

                } else {
                    msgDel.put(msg);
                }

            } catch (IOException e) {
                break;

            } catch (NullPointerException n) {
                System.out.println("Connection to Server lost");
                break;
            }
        }
    }
}
