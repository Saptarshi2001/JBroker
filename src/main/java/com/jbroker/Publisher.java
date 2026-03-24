/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jbroker;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author SAPTARSHI DUTTA
 */
public class Publisher {
    
    private List<Topic> lst= new CopyOnWriteArrayList<>();;
    public Publisher(List<Topic> lst) 
    {
        this.lst=lst;
    }

    String publish(String publishtopicname, Message msg) 
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
