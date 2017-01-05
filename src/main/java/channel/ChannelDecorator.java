package channel;

import java.io.IOException;

/**
 * Created by dev on 12/4/15.
 */
public abstract class ChannelDecorator implements IChannel {

    protected IChannel channel;

    public ChannelDecorator(IChannel channel) {
        this.channel = channel;
    }

    @Override
    public void send(byte[] data) throws IOException {
        channel.send(data);
    }

    @Override
    public byte[] receive() throws IOException {
        return channel.receive();
    }

    @Override
    public void close() throws IOException {
        channel.close();

    }

    @Override
    public boolean isClosed() {
        return channel.isClosed();
    }
}
