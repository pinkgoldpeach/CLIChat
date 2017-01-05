package client;

/**
 * Created by dev on 10/11/15.
 */
public class MessageDelivery {
    private String message;
    private boolean empty;
    private String lastMsg;

    public MessageDelivery(){
        this.message = "";
        this.empty = true;
        this.lastMsg = "No message received!";
    }

    public synchronized String take() {
        while (empty) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }
        empty = true;
        notifyAll();
        return message;
    }

    public synchronized void put(String message) {
        while (!empty) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }
        empty = false;
        this.message = message;
        notifyAll();
    }

    public void putLastMsg(String lastMsg){
        this.lastMsg = lastMsg;
    }

    public String getLastMsg(){
        return lastMsg;
    }
}
