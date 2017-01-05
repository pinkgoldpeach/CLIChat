package chatserver;

import java.io.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.security.PrivateKey;
import java.util.SortedMap;
import java.util.TreeMap;

import chatserver.threads.TCPListenerThread;
import chatserver.threads.UDPListenerThread;
import cli.Command;
import cli.Shell;
import util.Config;
import util.Keys;

public class Chatserver implements IChatserverCli, Runnable {

	private Config config;
	private Users users;
	private ServerSocket serverSocket;
    private DatagramSocket datagramSocket;
	private Shell shell;

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
	public Chatserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.config = config;

		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
	}

	@Override
	public void run() {


		try {
			File file = new File("keys/chatserver/chatserver.pem");
			PrivateKey privateKeyChatserver = Keys.readPrivatePEM(file);
			users = new Users(privateKeyChatserver);

		} catch (Exception e) {
			System.out.println("Error during loading private key");
			return;
		}


		try {
			serverSocket = new ServerSocket(config.getInt("tcp.port"));
			datagramSocket = new DatagramSocket(config.getInt("udp.port"));

			new TCPListenerThread(serverSocket, users).start();
			new UDPListenerThread(datagramSocket, users).start();
			new Thread(shell).start();

			System.out.println("Server is up!");

		} catch (IOException e) {
			throw new RuntimeException("Cannot listen to TCP or UDP Socket");
		}
	}

	@Override
	@Command
	public String users() throws IOException {
		SortedMap<String, String> user = new TreeMap<String, String>();

		for(String i : users.getUsers()) {
			i = i.replaceFirst(".password", "");
			user.put(i, users.getOnlineStatusByName(i));
		}
		String onlineUser = user.toString();
		onlineUser = onlineUser.replaceAll(", " , "\n").replaceAll("\\{", "").replaceAll("\\}", "");
		onlineUser = onlineUser.replace("=", " ");
		return onlineUser;
	}

	@Override
	@Command
	public String exit() throws IOException {
		if(serverSocket != null)
			serverSocket.close();
		if(datagramSocket != null) 
			datagramSocket.close();
		if(shell != null)
			shell.close();

		return "Shutdown completed";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Chatserver}
	 *            component
	 */
	public static void main(String[] args) {
		IChatserverCli chatserver = new Chatserver(args[0],
				new Config("chatserver"), System.in, System.out);
		new Thread((Runnable) chatserver).start();
	}

}
