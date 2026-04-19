/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jbroker.Subscribers;
import com.jbroker.Topic.Topic;
import com.jbroker.Clients.Client;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author SAPTARSHI DUTTA
 */
public class Subscriber {

    private List<Topic> lst = new CopyOnWriteArrayList<>();

    private Map<Integer, Topic> mp = new ConcurrentHashMap<>();

    public Subscriber(Map<Integer, Topic> mp, int subid, String topicname, List<Topic> lst) {
        this.lst = lst;
        this.mp = mp;
    }

    public String subscribe(int subid, Topic topic) {

        if (mp.containsKey(subid)) {
            return "Id already present";
        } else if (!mp.containsKey(subid)) {
            String topicname = topic.getTopicname();
            Client clnt = topic.getClient();

            for (Topic top : lst) {
                String temp = top.getTopicname();

                if (temp.equals(topicname)) {
                    List<Client> clients = top.getClients();

                    boolean found = false;
                    for (Client client : clients) {
                        if (client == clnt) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        top.add(clnt);
                    }

                    mp.put(subid, top);
                    return "Subscribed";
                }
            }

            // topic does not exist yet
            lst.add(topic);
            topic.add(clnt);
            mp.put(subid, topic);
            

        }

        return "Subscribed";
    }

    public String unsub(Client client, int subid) {

        int count = 0;
        if (!mp.containsKey(subid)) {
            return "Subscription not found";
        }
        for (Topic top : lst) {
            HashMap<Integer, Client> clntmap = top.getclntmap();

            for (Client cl : clntmap.values()) {
                if (cl.equals(client)) {
                    count++;
                }

                if (count > 1) {
                    break;
                }
            }

            if (count > 1) {
                break;
            }
        }

        mp.remove(subid);

        if (count <= 1) {
            for (Topic top : lst) {
                HashMap<Integer, Client> clntmap = top.getclntmap();

                if (clntmap.containsValue(client)) {
                    top.removeClient(client);
                }
            }
        }

        return "Unsubbed";
    }
}
