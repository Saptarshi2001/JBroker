
package com.jbroker.Publishers;
import com.jbroker.Topic.Topic;
import com.jbroker.Clients.Client;
import com.jbroker.Messages.Message;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.jbroker.Routers.Router;

public class Publisher {
    
    private List<Topic> lst= new CopyOnWriteArrayList<>();;
    public Publisher(List<Topic> lst) 
    {
        this.lst=lst;
    }

    public String publish(String publishtopicname, Message msg) 
    {
        String errmsg=null;
        String msgdesc=msg.getMessage();
        for(Topic top:lst)
        {
            String temp=top.getTopicname();
            if(temp.equals(publishtopicname))
            {
              List<Client> clnt=top.getClients();
              Router router=new Router(msg);
              for(Client client:clnt)
              {
                  
        router.sendclients(client,msgdesc); 
              }
              return "Published";
            }
            else
            {
                errmsg="No topic found";
                
            }
            
        }
        return errmsg;

    }
    
}
