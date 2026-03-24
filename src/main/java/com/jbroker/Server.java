
package com.jbroker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Server {
    
    private ServerSocket serversocket;
    private int serverport;
    private String host;
    private ThreadPool pool;
    private ProtocolParser parser;
    private List<Topic> lst= new CopyOnWriteArrayList<>();
    private Map<Integer,Topic>mp= new ConcurrentHashMap<>();;
    private static final Logger logger = Logger.getLogger("server.log");
    private Client globclient;
    public Server() 
    {    
        //parser=new ProtocolParser(mp,lst);
    }
    
    public void connect(String address) throws InterruptedException
    {
        try{
            
        String[] spltaddress=address.split(":");
        host=spltaddress[0];
        serverport=Integer.parseInt(spltaddress[1]);
        serversocket=new ServerSocket(serverport);
        System.out.println("System started");
        pool=new ThreadPool(4,10);
        while(true)
        {
            
            Socket clientsocket=serversocket.accept();
            String clientsocketaddr=clientsocket.getInetAddress().getHostAddress();
            int clientport=clientsocket.getPort();
            Client client=new Client(clientsocketaddr,clientport,clientsocket);
            globclient=client;
            String outgoingmsg = """
            Connected to client
            -------------------
            Server Info
            Host : %s
            Port : %d
            """.formatted(host, serverport);
                             
            Message msg=new Message("text",outgoingmsg.length(),outgoingmsg,client);
            Router router=new Router(msg);
            router.sendClient();
           
            pool.execute(()->{
                new ProtocolParser(mp,lst).parse(clientsocket,client);
            });
        }
        }catch(IOException ex)
        {
            logger.info(ex.getMessage());
            String outgoingmsg="System failed to connect";
            Message msg=new Message("text",outgoingmsg.length(),outgoingmsg,globclient);
            Router router=new Router(msg);
            router.sendClient();
            globclient=null;
            return;
        }
    }
    
}
