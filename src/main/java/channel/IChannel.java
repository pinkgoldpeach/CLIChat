package channel;


import java.io.IOException;

/**
 * Created by dev on 10/22/15.
 */
public interface IChannel {
    void send(byte[] data) throws IOException;

    byte[] receive() throws IOException;
    void close() throws IOException;
    boolean isClosed();
}