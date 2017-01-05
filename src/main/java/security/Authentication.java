package security;

import org.bouncycastle.util.encoders.Base64;
import util.Keys;

import javax.crypto.Cipher;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

/**
 * Created by dev on 12/6/15.
 */
public class Authentication {
    private Cipher cipher;
    private PrivateKey serverPrivateKey;

    public Authentication() {
        try {
            cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");

        } catch (Exception e) {
        }
    }

    public Authentication(PrivateKey serverPrivateKey) { // only for chatserver
        this();
        this.serverPrivateKey = serverPrivateKey;
    }

    public byte[] get_RSA_Message_Server(byte[] request) {

        try {
            cipher.init(Cipher.DECRYPT_MODE, serverPrivateKey);
            request = cipher.doFinal(request);
            return request;

        } catch (Exception e) {
            return null;
        }
    }

    public byte[] set_RSA_Message_Server(String username, byte[] message) {
        try {
            File file = new File("keys/chatserver/" + username + ".pub.pem");
            PublicKey publicKeyUser = Keys.readPublicPEM(file);
            cipher.init(Cipher.ENCRYPT_MODE, publicKeyUser);

            return cipher.doFinal(message);

        } catch (Exception e) {
            return null;
        }
    }

    public byte[] set_RSA_Message_Client(byte[] message) {
        try {
            File file = new File("keys/client/chatserver.pub.pem");
            PublicKey publicKeyChatserver = Keys.readPublicPEM(file);

            cipher.init(Cipher.ENCRYPT_MODE, publicKeyChatserver);

            return cipher.doFinal(message);
        } catch (Exception e) {
            return null;
        }
    }

    public byte[] get_RSA_Message_Client(String username, byte[] message) {
        try {
            File file = new File("keys/client/" + username + ".pem");
            PrivateKey privateKey = Keys.readPrivatePEM(file);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            return cipher.doFinal(message);

        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] getRandomBase64EncodedNumber(int size) {
        SecureRandom secureRandom = new SecureRandom();
        final byte[] number = new byte[size];
        secureRandom.nextBytes(number);
        byte[] number2 = Base64.encode(number);

        return number2;
    }

}
