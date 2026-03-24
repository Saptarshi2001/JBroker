/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jbroker;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.logging.Logger;

/**
 *
 * @author SAPTARSHI DUTTA
 */
public class Router {
    
    private Message msg;
    //Client client;
    private static final Logger logger = Logger.getLogger("router.log");
    public Router(Message msg) 
    {
        this.msg=msg;
    }
    
    
    public void sendClient() {
        try{
        Client client=msg.getClient();
        Socket clientsocket=client.getSocket();
        BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(clientsocket.getOutputStream()));
        String message=msg.getMessage();
        writer.write(message+"\n");
        writer.flush();
        }catch(IOException ex)
        {
            System.out.println("IOException occured!!!");
            logger.info(ex.getMessage());
            return;
        }
    }
    
    public void sendclients(Client client,String msgdesc)
    {
        try
        {
        Socket clientsocket=client.getSocket();
        BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(clientsocket.getOutputStream()));
        writer.write(msgdesc+"\n");
        writer.flush();
        }catch(IOException ex)
        {
            System.out.println("IOException occured!!!");
            logger.info(ex.getMessage());
            return;
        }catch(ArrayIndexOutOfBoundsException ex)
        {
           System.out.println("ArrayIndexOutOfBoundsException occured!!!");
            logger.info(ex.getMessage());
            return; 
        }
        
    }

    
    
    
}
