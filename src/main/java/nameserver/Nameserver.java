package nameserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import cli.Command;
import cli.Shell;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import util.Config;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class Nameserver implements INameserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private String root_id;
	private String host;
	private int port;
	private String domain;
	private Registry registry;
	private Shell shell;
	private RegisterNameserver registerNameserver;
	private boolean success;


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
	public Nameserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		this.success = false;

		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);

		/**
		 * reads the following parameters from the ns-X.properties config file
		 */
		this.root_id = config.getString("root_id");
		this.host = config.getString("registry.host");
		this.port = config.getInt("registry.port");

		/**
		 * Check if the started nameserver is the root nameserver. rootserver doesn't contain "domain"
		 * config doesn't return null, it returns an exception, if domain doesn't exist
		 */
		try{
			this.domain = config.getString("domain");
		}catch (MissingResourceException m){
			this.domain = null;
		}

		/**
		 * Create INameserver-Object
		 */

		this.registerNameserver = new RegisterNameserver(domain);

		if(isRootServer()){
			try {
				// create and export the registry instance on localhost at the specified port
				registry = LocateRegistry.createRegistry(this.port);

				// create a remote object of this INameserver object
				INameserver remote = (INameserver) UnicastRemoteObject.exportObject(registerNameserver, 0);

				// bind the obtained remote object on specified binding name in the registry
				registry.bind(this.root_id, remote);
				this.success = true;

			} catch (RemoteException e) {
				throw new RuntimeException("Error while starting server.", e);
			} catch (AlreadyBoundException e) {
				throw new RuntimeException(
						"Error while binding remote object to registry.", e);
			}
		}else{
			try {
				// obtain registry that was created by the root-server
				registry = LocateRegistry.getRegistry(this.host, this.port);
				// create a remote object of this INameserver object
				INameserver remote = (INameserver) UnicastRemoteObject.exportObject(registerNameserver, 0);
				// look for the bound server remote-object implementing the INameserver interface
				INameserver root = (INameserver) registry.lookup(root_id);

				root.registerNameserver(domain, remote, remote);
				this.success = true;

			} catch (RemoteException e) {
				throw new RuntimeException(
						"Error while obtaining registry/server-remote-object.", e);
			} catch (NotBoundException e) {
				throw new RuntimeException(
						"Error while looking for server-remote-object.", e);
			} catch (AlreadyRegisteredException e) {
				e.printStackTrace();
			} catch (InvalidDomainException e) {
				System.out.println(e.getMessage());

			}
		}
	}

	// returns if this server is a root server
	public boolean isRootServer(){
		return (domain == null) ? true : false;
	}


	@Override
	public void run() {
		if(this.success) {
			System.out.println(this.componentName + " is up!");
			new Thread(shell).start();
		}else{
			try {
				exit();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Prints out each known nameserver (zones) in alphabetical order, from the perspective of this nameserver.
	 * Synchronize the data structures you use to manage your subdomains.
	 * ConcurrentSkipListMap == concurrent equivalent to TreeMap (alphabetical order, synchronized)
     */

	@Override
	@Command("!nameservers")
	public String nameservers() throws IOException {

		ConcurrentSkipListMap<String, INameserver> nameservers = registerNameserver.getNameservers();
		Set<String> domains = nameservers.keySet();
		String nameServerDomains = new String();
		int counter = 1;
		if(!domains.isEmpty()) {
			for (String domain : domains) {
				nameServerDomains += counter + ".  " + domain + "\n";
				counter++;
			}
		}else{
			nameServerDomains = "No further nameservers registerd on " + this.componentName;
		}
		return nameServerDomains;
	}

	/**
	 * Prints out some information about each stored address, containing username and address (IP:port),
	 * arranged by the username in alphabetical order
	 * Returns registered users of a domain - or "no users registered"
     */
	@Override
	@Command("!addresses")
	public String addresses() throws IOException {
		ConcurrentSkipListMap<String, String> registeredUsers = registerNameserver.getRegisteredUsers();
		Set<String> usernames = registeredUsers.keySet();
		String usersIpPort = new String();
		int counter = 1;
		if (!usernames.isEmpty()) {
			for (String username : usernames) {
				usersIpPort += counter + ".  " + username + " " + registeredUsers.get(username) + "\n";
				counter++;
			}
		}else{
			usersIpPort = "No users registered on " + this.componentName;
		}
		return usersIpPort;
	}

	/**
	 * Shutdown the nameserver.
     */
	@Override
	@Command("!exit")
	public String exit() throws IOException {

		if(isRootServer()){
			try {
				registry.unbind(this.root_id);
			} catch (NotBoundException e) {
				e.printStackTrace();
			}
		}
		UnicastRemoteObject.unexportObject(registerNameserver, true);
		if(shell != null)
			shell.close();

		return this.componentName + " shutting down";
	}

	@Override
	public String toString(){
		return "Nameserver: "+ componentName + ", "+ root_id + ", "+ host;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Nameserver}
	 *            component
	 */
	public static void main(String[] args) {
		Nameserver nameserver = new Nameserver(args[0], new Config(args[0]),
				System.in, System.out);
		nameserver.run();
	}

}
