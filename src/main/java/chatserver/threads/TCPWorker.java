package chatserver.threads;

import channel.IChannel;
import chatserver.Users;
import nameserver.INameserverForChatserver;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import org.bouncycastle.util.encoders.Base64;
import security.AES_Channel;
import security.Authentication;
import util.ByteUtil;
import util.Config;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Created by dev on 10/9/15.
 */
public class TCPWorker implements Runnable {
    private IChannel tcpChannel;
    private Users users;
    private Registry registry;
    private INameserverForChatserver root;
    private AES_Channel aes_channel;


    public TCPWorker(IChannel tcpChannel, Users users) {
        this.tcpChannel = tcpChannel;
        this.users = users;

        Config rootConfig = new Config("ns-root");
        String host = rootConfig.getString("registry.host");
        int port = rootConfig.getInt("registry.port");
        String root_id = rootConfig.getString("root_id");
        try {
            this.registry = LocateRegistry.getRegistry(host, port);
            this.root = (INameserverForChatserver) registry.lookup(root_id);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            byte[] request;

            while ((request = tcpChannel.receive()) != null) {

                try {
                    Authentication authentication = new Authentication(users.getPrivateKey());
                    request = authentication.get_RSA_Message_Server(request);
                    Deque<byte[]> messages = ByteUtil.getWords(request);

                    String command = new String(messages.poll());

                    if ("!authenticate".matches(command)) {

                        String username = new String(messages.poll());

                        byte[] tmp = Base64.encode("!ok ".getBytes());
                        byte[] user_challenge = messages.poll();
                        byte[] challenge = Authentication.getRandomBase64EncodedNumber(32);
                        byte[] secret_key = Base64.encode(AES_Channel.getAESSecretKey());
                        byte[] iv = Authentication.getRandomBase64EncodedNumber(16);

                        Deque<byte[]> message = new LinkedList<>();
                        message.add(tmp);
                        message.add(user_challenge);
                        message.add(challenge);
                        message.add(secret_key);
                        message.add(iv);

                        tcpChannel.send(authentication.set_RSA_Message_Server(username, ByteUtil.setWords(message)));

                        aes_channel = new AES_Channel(tcpChannel, Base64.decode(secret_key), Base64.decode(iv));

                        if (!ByteUtil.isEqual(aes_channel.receive(), challenge)) {
                            System.out.println("Challenge ist nicht ident");
                            aes_channel.send("Server hat falsche Challenge bekommen, Verbindung wird geschlossen".getBytes());
                            aes_channel.close();
                        } else {
                            aes_channel.send("AES Channel erfolgreich aufgebaut".getBytes());
                            users.setUserOnline(aes_channel, username);
                            break;
                        }
                    }

                } catch (Exception e) {

                }
            }

            while ((request = aes_channel.receive()) != null) {

                String message = new String(request);

                String command = (message.split(" "))[0];
                String msg = message.substring(message.indexOf(" ") + 1, message.length());


                if ("!logout".equals(command)) {
                    users.setUserOffline(aes_channel);
                    aes_channel.send("Successfully logged out.".getBytes());
                    aes_channel.close();

                } else if ("!send".equals(command)) {
                    for (AES_Channel ch : users.getOnlineUsers().keySet()) {
                        if (ch != aes_channel) {
                            String tmp = "message " + users.getOnlineUserByChannel(aes_channel) + ": " + msg;
                            ch.send(tmp.getBytes());
                        }
                    }
                    aes_channel.send("Message sent".getBytes());

                } else if ("!register".equals(command)) {
                    try {
                        root.registerUser(users.getOnlineUsers().get(aes_channel), msg);
                        String tmp = "Successfully registered address for " + users.getOnlineUserByChannel(aes_channel);
                        aes_channel.send(tmp.getBytes());
                    } catch (AlreadyRegisteredException a) {
                        aes_channel.send(a.getMessage().getBytes());
                    } catch (InvalidDomainException i) {
                        aes_channel.send(i.getMessage().getBytes());
                    } catch (Exception e) {
                        aes_channel.send("Error during registering user".getBytes());
                    }

                } else if ("!lookup".equals(command)) {
                    INameserverForChatserver server = root;
                    String address = "";

                    try {
                        if (msg.contains(".")) {
                            String domains[] = msg.split("\\.");
                            for (int i = domains.length - 1; i > 0; i--) {
                                server = server.getNameserver(domains[i]);
                            }
                            address = server.lookup(domains[0]);
                        }
                        aes_channel.send(address.getBytes());
                    } catch (Exception e) {
                        aes_channel.send("Error looking up user".getBytes());
                    }
                }
            }

        } catch (Exception e) {
        }
    }
}
