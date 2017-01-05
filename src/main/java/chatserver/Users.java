package chatserver;

import security.AES_Channel;
import util.Config;

import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Patrick on 10/9/15.
 */
public class Users {

    private Config user;
    private ConcurrentHashMap<AES_Channel, String> onlineUsers = new ConcurrentHashMap<>();

    private PrivateKey privateKey; // Private Key des Chatserver wird bei Start geladen

    public Users(PrivateKey privateKey) {
        user = new Config("user");
        this.privateKey = privateKey;
    }
    public Set<String> getUsers(){ return user.listKeys(); }

    public Map<AES_Channel, String> getOnlineUsers() {
        return onlineUsers;
    }

    public void setUserOnline(AES_Channel aes_channel, String name) {
        onlineUsers.putIfAbsent(aes_channel, name);
    }

    public void setUserOffline(AES_Channel aes_channel) {
        onlineUsers.remove(aes_channel);
    }

    public String getOnlineStatusByName(String name){
        if(onlineUsers.containsValue(name))
            return "online";
        else
            return "offline";
    }

    public String getOnlineUserByChannel(AES_Channel aes_channel) {
        return onlineUsers.get(aes_channel);
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}

