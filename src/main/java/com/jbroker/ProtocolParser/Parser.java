
package com.jbroker.ProtocolParser;
import com.jbroker.Topic.Topic;
import com.jbroker.Messages.Message;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import com.jbroker.Routers.Router;
import com.jbroker.Clients.Client;
import com.jbroker.Subscribers.Subscriber;
import com.jbroker.Publishers.Publisher;

public class Parser {

    private Map<Integer,Topic> mp;
    private List<Topic> lst;
    private static final Logger logger = Logger.getLogger("server.log");
    boolean flag=false;
    boolean connectflg=false;
    Message publishmsg;
    String publishtopicname;
    public Parser(Map<Integer, Topic> mp, List<Topic> lst) 
    {
        this.mp=mp;
        this.lst=lst;
    }
    
    
    public void parse(Socket clientsocket, Client client)
    {
        try{
        BufferedReader input=new BufferedReader(new InputStreamReader(clientsocket.getInputStream()));
        String inputmsg;
        while( (inputmsg=input.readLine())!=null)
        {
        String[] rcvmsg=inputmsg.split(" ");
        boolean valid=validate(inputmsg);
        if(rcvmsg[0].equals("Connect"))
        {
            if(valid==true)
            {
            String connectmsg="+OK"; 
            Message msg=new Message("text",connectmsg.length(),connectmsg,client);
            Router router=new Router(msg);
            router.sendClient();
            connectflg=true;
            }
            else
            {
        
        String errmsg="Wrong Connect !!! Please type Connect{}";
        Message msg=new Message("text",errmsg.length(),errmsg,client);
        Router router=new Router(msg);
        router.sendClient();
            
        

            }
        }
        
        else if(rcvmsg[0].equals("Sub"))
        {
            if(connectflg==false)
            {
               String errmsg="First use Connect {}";
        Message msg=new Message("text",errmsg.length(),errmsg,client);
        Router router=new Router(msg);
        router.sendClient();  
            }
            else if(valid==true)
            {
            subscribe(rcvmsg,client);
            }
            
            else
        {
        String errmsg="Invalid sub message !!! Please type Sub [topic_name] [subscriber_id]";
        Message msg=new Message("text",errmsg.length(),errmsg,client);
        Router router=new Router(msg);
        router.sendClient(); 
        }
        }
        
        else if(rcvmsg[0].equals("Pub"))
        {
            if(connectflg==false)
            {
               String errmsg="First use Connect {}";
        Message msg=new Message("text",errmsg.length(),errmsg,client);
        Router router=new Router(msg);
        router.sendClient();  
            }
            else if(valid==true)
            {
            publishtopicname=rcvmsg[1];
              publishcomposemsg(client,Integer.parseInt(rcvmsg[2]));
            }
            else
            {
         String errmsg="Invalid pub message !!! Please type Pub [Topic_name][message_length]";
        Message msg=new Message("text",errmsg.length(),errmsg,client);
        Router router=new Router(msg);
        router.sendClient();

            }
        
        }
        
        else  if(flag==true)
         {
             publish(rcvmsg[0]);
         }
         
       else if(rcvmsg[0].equals("Unsub"))
        {
            if(valid==true)
            {
              unsub(rcvmsg,client,Integer.parseInt(rcvmsg[1]));
            }
             else
        {
        String errmsg="Invalid unsub message !!! Please type Unsub [Topic_name][Number of messages after which to unsub](Optional)";
        Message msg=new Message("text",errmsg.length(),errmsg,client);
        Router router=new Router(msg);
        router.sendClient(); 
            
        }
        }

        else 
        {
        String errmsg="Invalid message.Choose Connect,Sub,Pub,Unsub !!!";
        Message msg=new Message("text",errmsg.length(),errmsg,client);
        Router router=new Router(msg);
        router.sendClient();  
        }
       
        }
        }catch(IOException ex){
            logger.info(ex.getMessage());
            System.out.println("System failed to connect");
            return;
        }
        
    }
    
    public boolean validate(String msg)
    {
        try{
        boolean valid=false;
        String[] spltmsg=msg.split(" ");
        if(spltmsg[0].equals("Connect") && spltmsg[1].equals("{}"))
        {
            
        valid=true;
        }
        
        
        else if(spltmsg[0].equals("Sub")&& spltmsg[1].matches("^[a-zA-Z0-9.*]+$") && spltmsg[2].matches("^[0-9]+$"))
        {
            valid=true;
        }
    
        else if(spltmsg[0].equals("Pub")&& spltmsg[1].matches("^[a-zA-Z0-9.]+$") && spltmsg[2].matches("^[0-9]+$"))
        {
            valid=true;
        }
        
        else if(spltmsg[0].equals("Unsub")&& spltmsg[1].matches("^[0-9]+$"))
        {
            
            valid=true;
        }
        else {
            valid=false;
        }
        
        return valid;
        }catch(ArrayIndexOutOfBoundsException ex)
        {
           logger.info(ex.getMessage());
            System.out.println("System failed to connect");
            return false; 
        }
    }

    public void subscribe(String[] rcvmsg,Client client) 
    {
        String topicname=rcvmsg[1];
        int subid=Integer.parseInt(rcvmsg[2]);
        Subscriber subscriber=new Subscriber(mp,subid,topicname,lst);
        Topic topic=new Topic(topicname,client,subid);
        String submsg=subscriber.subscribe(subid,topic);
        
        if(submsg.equals("Subscribed"))
        {
        Message message;
        message = new Message("text",submsg.length(),submsg,client);
        Router router=new Router(message);
        router.sendClient();
        }
        else
        {
        Message errmessage;
        errmessage = new Message("text",submsg.length(),submsg,client);
        Router router=new Router(errmessage);
        router.sendClient();    
        }
        
        
    }

    public void publishcomposemsg(Client client,int length) 
    {
        publishmsg=new Message("text",length,null,client);
        flag=true;
    }
    
    public void publish(String msg)
    {
        publishmsg.setMessage(msg);
        
        Publisher publisher=new Publisher(lst);
        String result=publisher.publish(publishtopicname,publishmsg);
        publishmsg=null;
        publishtopicname=null;
    }

    public void unsub(String[] rcvmsg,Client client,int subid) 
    {
        //String topicname=rcvmsg[1];
        Subscriber subscriber=new Subscriber(mp,subid,null,lst);
        String resultmsg=subscriber.unsub(client,subid);
        if(resultmsg.equals("Unsubbed"))
        {
        Message message;
        message = new Message("text",resultmsg.length(),resultmsg,client);
        Router router=new Router(message);
        router.sendClient();
        }
        else
        {
        Message errmessage;
        errmessage = new Message("text",resultmsg.length(),resultmsg,client);
        Router router=new Router(errmessage);
        router.sendClient();    
        }
        
    }
}
