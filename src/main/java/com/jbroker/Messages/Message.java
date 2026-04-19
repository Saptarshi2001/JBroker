package com.jbroker.Messages;

import com.jbroker.Clients.Client;


public class Message implements SendMessage{

    private String msg, type;
    private int length;
    private Client client;

    public Message(String type,int length,String msg,Client client) {
        
        this.type = type;
        this.length = length;
        this.msg = msg;
        this.client=client;
        
    }

    public String getType() {
        return type;
    }
    
    public void setType(String type)
    {
        this.type=type;
    }
    
    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
    
    public String getMessage() {
        return msg;
    }

    public void setMessage(String msg) {
        this.msg = msg;
    }
    
    public Client getClient()
    {
        return client;
    }
    
    public void setClient(Client client)
    {
        this.client=client;
    }

    @Override
    public void sendmessageText(String msg) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
