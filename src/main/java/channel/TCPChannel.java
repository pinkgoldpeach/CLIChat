package channel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by dev on 10/22/15.
 */
public class TCPChannel implements IChannel{
    private Socket socket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;

    public TCPChannel(Socket socket){
        this.socket = socket;

    }
    @Override
    public void send(byte[] data) throws IOException {
        printWriter = new PrintWriter(socket.getOutputStream(), true);
        printWriter.println(new String(data));
    }

    @Override
    public byte[] receive() throws IOException {
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return bufferedReader.readLine().getBytes();
    }

    @Override
    public void close() throws IOException{
        socket.close();
    }

    @Override
    public boolean isClosed(){
        if(socket.isClosed())
            return true;
        return false;
    }
}
