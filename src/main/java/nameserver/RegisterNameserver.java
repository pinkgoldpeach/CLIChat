package nameserver;

import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * This class implements all further interfaces for the nameservers
 * registers further nameservers, and communicates with the chatserver
 * these objects are also remote object.
 * INameserver extends INameserverForChatserver, Remote
 */

public class RegisterNameserver implements INameserver {

    private String domain;
    private ConcurrentSkipListMap<String, INameserver> callbacks;
    private ConcurrentSkipListMap<String, String> registeredUsers;

    /**
     * callbacks contains next-level servers (top-down)
     * registeredUsers contains the users registered at this server
     */

    public RegisterNameserver(String domain){
        this.domain = domain;
        this.callbacks = new ConcurrentSkipListMap<>();
        this.registeredUsers = new ConcurrentSkipListMap<>();
    }

    /**
     * this method always starts at the rootserver. first call in contructor of nameserver, to register servers.
     * @param domain == name of server e.g. "berlin.de"
     * @param nameserver == the server that needs to be registered
     * @param nameserverForChatserver == nameserver
     * AlreadyRegisteredException = desired domain is already in use
     * InvalidDomainException = a zone the request has to be routed to does not exist
     * if domain contains subdomains, split it and check for basic servers . e.g. "berlin.de" - check if "de" exists as subdomain
     * of root, then check for "berlin" - register if berlin doesn't exist.
     */
    @Override
    public void registerNameserver(String domain, INameserver nameserver, INameserverForChatserver nameserverForChatserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        if(domain.contains(".")){
            String domains[] = domain.split("\\.");
            if(domains.length > 1){
                String currentDomain = domains[domains.length-1];
                // merge all the remaining domains into one string, for further recursion
                String remainingDomains = "";
                for(int i = 0; i < domains.length-1; i++) {
                    remainingDomains += domains[i] +".";
                }
                remainingDomains = remainingDomains.substring(0, remainingDomains.length()-1);
                if(callbacks.containsKey(currentDomain)){
                    //do something with this server
                    callbacks.get(currentDomain).registerNameserver(remainingDomains,nameserver, nameserverForChatserver);
                    /**
                     * if one of the basic servers couldn't be found, throw exception, don't
                     * initialise new server. example berlin.de : "de" has to be initialised before "berlin".
                     */
                }else{
                    throw new InvalidDomainException("server \"" + currentDomain + "\" could not be found");
                }
            }
        }else if(domain.length() > 1){
            if(!callbacks.containsKey(domain)){
                System.out.println("Registering nameserver for zone '"+domain+"'");
                callbacks.put(domain, nameserver);
            }else{
                throw new AlreadyRegisteredException("The server: " + domain + " already exists");
            }
        }
    }

    /**
     * Register the private address of a user
     * @throws AlreadyRegisteredException == if user is already registered
     * @throws InvalidDomainException == subdomain doesn't exist
     */

    @Override
    public void registerUser(String username, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        if(username.contains(".")){
            String domains[] = username.split("\\.");
            if(domains.length > 1){
                String currentDomain = domains[domains.length-1];
                // merge all the remaining domains into one string, for further recursion
                String remainingDomains = "";

                for(int i = 0; i < domains.length-1; i++) {
                    remainingDomains += domains[i]+".";
                }

                remainingDomains = remainingDomains.substring(0, remainingDomains.length()-1);
                if(callbacks.containsKey(currentDomain)){
                    //do something with this server
                    callbacks.get(currentDomain).registerUser(remainingDomains, address);
                    /**
                     * if one of the basic servers couldn't be found, throw exception, don't
                     * initialise new server. example berlin.de : "de" has to be initialised before "berlin".
                     */
                }else{
                    throw new InvalidDomainException("server \"" + currentDomain + "\" could not be found");
                }
            }
        }else if(username.length() > 1){
            if(!registeredUsers.containsKey(username)){
                registeredUsers.put(username, address);
            }else{
                throw new AlreadyRegisteredException("This user: " + username + " is already registered");
            }
        }
    }

    @Override
    public INameserverForChatserver getNameserver(String zone) throws RemoteException {
        System.out.println("Nameserver for " + zone +" ' requested by chatserver");
        if(callbacks.containsKey(zone)){
            return callbacks.get(zone);
        }else{
            throw new RemoteException("The zone: \"" + zone + "\" has no server");
        }

    }

    /**
     * Lookup the private address of a user
     * @param username lookup the address for the user
     */

    @Override
    public String lookup(String username) throws RemoteException {
        if(registeredUsers.containsKey(username)){
            //System.out.println("username: "+ username + " address "+registeredUsers.get(username));
            return registeredUsers.get(username);
        }else{
            //System.out.println("no such user "+ username);
            return "The name is not registered";
        }
    }

    @Override
    public String toString(){
        return "RegisterNameServer: " + domain;
    }

    public ConcurrentSkipListMap<String, INameserver> getNameservers(){
        return callbacks;
    }

    public ConcurrentSkipListMap<String, String> getRegisteredUsers(){
        return registeredUsers;
    }
}
