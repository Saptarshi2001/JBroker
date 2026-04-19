
package com.jbroker.Topic;
import com.jbroker.Clients.Client;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Topic
{
    Client client;
    String topicname;
    int subid;
    List<Client> list = new CopyOnWriteArrayList<>();
    HashMap<Integer,Client> clntmap=new HashMap<>();
    public Topic(String topicname,Client client,int subid)
    {
     this.subid=subid;
     this.topicname=topicname;
     this.client=client;
     
    }

    public HashMap<Integer,Client> getclntmap()
    {
        return clntmap;
    }
    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getTopicname() {
        return topicname;
    }

    public void setTopicname(String topicname) {
        this.topicname = topicname;
    }

    public List<Client> getClients()
    {
        return list;
    }
    
    public void setClients(List<Client> clientlst)
    {
        this.list=clientlst;
    }
    

    public void add(Client client) {

        list.add(client);
    }
    
    public void addClientMap(Client client)
    {
        clntmap.put(subid, client);
    }

    public void removeClient(Client client) {
        list.remove(client);
    }
}