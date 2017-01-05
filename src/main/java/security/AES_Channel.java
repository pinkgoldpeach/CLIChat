package security;

import channel.IChannel;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;

/**
 * Created by dev on 12/6/15.
 */
public class AES_Channel {
    private IChannel channel;
    private Cipher cipher;
    private byte[] secretKey;
    private byte[] iv;

    public AES_Channel(IChannel channel, byte[] secretKey, byte[] iv) throws AES_Channel_Exception {
        this.channel = channel;
        this.secretKey = secretKey;
        this.iv = iv;

        try {
            cipher = Cipher.getInstance("AES/CTR/NoPadding");
        } catch (Exception e) {
            throw new AES_Channel_Exception();
        }
    }

    public void send(byte[] message) throws IOException {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secretKey, "AES"), new IvParameterSpec(iv));
            channel.send(cipher.doFinal(message));

        } catch (Exception e) {
            throw new IOException();
        }

    }

    public byte[] receive() throws IOException {
        try {
            byte[] message = channel.receive();
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secretKey, "AES"), new IvParameterSpec(iv));
            return cipher.doFinal(message);

        } catch (Exception e) {
            throw new IOException();
        }
    }

    public void close() throws IOException {
        try {
            channel.close();
        } catch (IOException i) {

        }

    }

    public boolean isClosed() {
        return channel.isClosed();
    }

    public static byte[] getAESSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();

            return secretKey.getEncoded();
        } catch (Exception e) {
            return null;
        }
    }
}
