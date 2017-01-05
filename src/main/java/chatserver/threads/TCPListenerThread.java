package chatserver.threads;

import channel.Base64Channel;
import channel.IChannel;
import channel.TCPChannel;
import chatserver.Users;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by dev on 10/9/15.
 */
public class TCPListenerThread extends Thread {
    private ServerSocket serverSocket;
    private Users users;

    private Set<IChannel> connections = new HashSet<>();

    public TCPListenerThread(ServerSocket serverSocket, Users users) {
        this.serverSocket = serverSocket;
        this.users = users;
    }

    public void run(){
        ExecutorService executorService = Executors.newFixedThreadPool(30);
        while(true){
            try {
                IChannel tcpChannel = new Base64Channel(new TCPChannel(serverSocket.accept()));
                connections.add(tcpChannel);
                Runnable tcpworker = new TCPWorker(tcpChannel, users);
                executorService.execute(tcpworker);
            } catch (IOException e){
                executorService.shutdown();
                for(IChannel s : connections){
                    if(s != null)
                        try {
                            s.close();
                        } catch (IOException ei){}
                }
                break;
            }
        }
    }
}
