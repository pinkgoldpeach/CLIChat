package channel;


import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;

/**
 * Created by dev on 12/4/15.
 */
public class Base64Channel extends ChannelDecorator {

    public Base64Channel(IChannel channel) {
        super(channel);
    }

    @Override
    public void send(byte[] data) throws IOException {
        byte[] base64Message = Base64.encode(data);
        super.send(base64Message);
    }

    @Override
    public byte[] receive() throws IOException {
        byte[] data = super.receive();
        return Base64.decode(data);
    }

}
