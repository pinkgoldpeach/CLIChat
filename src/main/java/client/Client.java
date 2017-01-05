package client;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Deque;
import java.util.LinkedList;
import java.security.Key;
import javax.crypto.Mac;


import channel.Base64Channel;
import channel.IChannel;
import channel.TCPChannel;
import channel.UDPChannel;
import cli.Command;
import cli.Shell;
import client.threads.TCPListenerThread;
import client.threads.UDPListenerThread;
import client.threads.TCPListenerThreadPrivate;
import org.bouncycastle.util.encoders.Base64;
import security.AES_Channel;

import security.Authentication;
import util.ByteUtil;
import util.Config;
import util.Keys;
import util.SecurityUtils;


public class Client implements IClientCli, Runnable {

	private Config config;

	private Shell shell;
	private ServerSocket serverSocket;

	private MessageDelivery msgDel;

    private IChannel tcpChannel;
    private IChannel udpChannel;

    private AES_Channel aes_channel;


	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.config = config;
		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
	}

	@Override
	public void run() {
		try{
            SecurityUtils.registerBouncyCastle();


            msgDel = new MessageDelivery();

            tcpChannel = new Base64Channel(new TCPChannel(new Socket(config.getString("chatserver.host"), config.getInt("chatserver.tcp.port"))));
            udpChannel = new Base64Channel(new UDPChannel(config.getString("chatserver.host"), config.getInt("chatserver.udp.port")));

            new UDPListenerThread(udpChannel, msgDel).start();
            new Thread(shell).start();

			System.out.println("Client is up!");


		} catch (IOException e) {
			System.out.println("Cannot bind to TCP or UDP Socket");
		}
	}

	@Override
	@Command
	public String login(String username, String password) throws IOException {
        return "login Befehl wird nicht mehr unterstützt";
    }

	@Override
	@Command
	public String logout() throws IOException {
        aes_channel.send("!logout".getBytes());
        String logout = msgDel.take();
        aes_channel.close();
        return logout;
	}

	@Override
	@Command
	public String send(String message) throws IOException {
        aes_channel.send(("!send " + message).getBytes());
        return msgDel.take();
	}

	@Override
	@Command
	public String list() throws IOException {
        udpChannel.send("!list".getBytes());
        return "Online Users:\n" + msgDel.take();
	}

	@Override
	@Command
	public String msg(String username, String message) throws IOException {
		String ip = lookup(username);

		if(("You are not logged in".matches(ip)) || ("The name is not registered".matches(ip)))
			return ip;

		String outgoingHash;

		File file;
		Key secretKey;
		try {
			//initialising MAC with the secret key
			file = new File(config.getString("hmac.key"));
			secretKey =  Keys.readSecretKey(file);

			Mac hMac = Mac.getInstance("HmacSHA256");
			hMac.init(secretKey);
			// MESSAGE is the message to sign in bytes
			byte[] hash = Base64.encode(hMac.doFinal(("!msg "+message).getBytes()));
			//hMac.update(message.getBytes());
			//byte[] hash = Base64.encode(hMac.doFinal());

			outgoingHash = new String(hash);
		} catch (Exception e) {
			return "something happened while working on the HMAC";
		}
		String[] address = ip.split(":");
		Socket skt = new Socket(address[0], Integer.parseInt(address[1]));
        IChannel channel = new Base64Channel(new TCPChannel(skt));
        channel.send((outgoingHash+" !msg "+message).getBytes());
        String ack = new String(channel.receive());
		channel.close();
		skt.close();
		String[] streamparts = ack.split(" ",3);
		String receivedHMAC = streamparts[0];
		String receivedCommand = streamparts[1];
		String receivedMsg = streamparts[2];


			try {
				Mac hMac = Mac.getInstance("HmacSHA256");
				hMac.init(secretKey);
				if(receivedCommand.equals("!ack")) {
					byte[] computeHash = Base64.encode(hMac.doFinal(("!ack " + receivedMsg).getBytes()));
					if (MessageDigest.isEqual(receivedHMAC.getBytes(), computeHash)) {
						return "!ack";
					} else {
						return "answer from "+username+" got manipulated";
					}
				} else if (receivedCommand.equals("!tampered")) {
					byte[] computeHash = Base64.encode(hMac.doFinal(("!tampered " + receivedMsg).getBytes()));
					if (MessageDigest.isEqual(receivedHMAC.getBytes(), computeHash)) {
						return "the message to "+username+" got manupulated";
					} else {
						return "the message to " + username + " got manupulated and the answer too.";
					}
				} else {
					return "answer from "+username+" got manipulated";
				}
			} catch (Exception ex){
				return "something wrong while generating the HMAC";
			}


	}

	@Override
	@Command
	public String lookup(String username) throws IOException {
        aes_channel.send(("!lookup " + username).getBytes());
        return msgDel.take();
	}

	@Override
	@Command
	public String register(String privateAddress) throws IOException {
		try {
			String[] address = privateAddress.split(":");
			serverSocket = new ServerSocket(Integer.parseInt(address[1]));
			new TCPListenerThreadPrivate(serverSocket).start();
		} catch (IOException e) {
			return "Registration not possible, use another port?";
		}

        aes_channel.send(("!register " + privateAddress).getBytes());
        String message = msgDel.take();
		if(message.matches("You are not logged in"))
			serverSocket.close();

		return message;
	}
	
	@Override
	@Command
	public String lastMsg() throws IOException {
		return msgDel.getLastMsg();
	}

	@Override
	@Command
	public String exit() throws IOException {
		logout();

		if(tcpChannel != null)
			tcpChannel.close();
        if(udpChannel != null)
            udpChannel.close();
		if(serverSocket != null)
			serverSocket.close();
		if(shell != null)
			shell.close();

		return "Shutdown completed";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		IClientCli client = new Client(args[0], new Config("client"), System.in,
				System.out);
		new Thread((Runnable) client).start();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
    @Command
    public String authenticate(String username) throws IOException {

        try {
            if (!new File("keys/client/" + username + ".pem").exists())
                return "Dieser Username ist nicht vorhanden";

            Authentication authentication = new Authentication();

            byte[] challenge = Authentication.getRandomBase64EncodedNumber(32);

            Deque<byte[]> deque = new LinkedList<>();
            deque.add("!authenticate".getBytes());
            deque.add(username.getBytes());
            deque.add(challenge);
            byte[] message = ByteUtil.setWords(deque);

            tcpChannel.send(authentication.set_RSA_Message_Client(message));

            byte[] message2 = tcpChannel.receive();

            message2 = authentication.get_RSA_Message_Client(username, message2);
            deque = ByteUtil.getWords(message2);

            if ("!ok".matches(new String(Base64.decode(deque.poll()))))
                return "Fehler bei Handshake";


            if (!ByteUtil.isEqual(challenge, deque.poll())) {
                return "Server schickt falsche Challenge zurück. Handshake abgebrochen";
            }

            byte[] challenge2 = deque.poll();

            aes_channel = new AES_Channel(tcpChannel, Base64.decode(deque.poll()), Base64.decode(deque.poll()));
            aes_channel.send(challenge2);

            new TCPListenerThread(aes_channel, msgDel).start();

            return new String(aes_channel.receive());

        } catch (Exception r) {
            return "Authentifizierung fehlgeschlagen";
        }

    }
}
