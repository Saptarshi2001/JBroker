/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jbroker.Clients
        ;

import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author SAPTARSHI DUTTA
 */
public class Client {

    String clientaddr;
    int clientport;
    Socket socket;
    List<String> lst = new CopyOnWriteArrayList<>();
    //Message msg;
    public Client(String addr, int port, Socket clientsocket) 
    {
        this.clientaddr=addr;
        this.clientport=port;
        this.socket=clientsocket;
                
    }
    
    // Getter methods
    public String getClientaddr() {
        return clientaddr;
    }
    
    public int getClientport() {
        return clientport;
    }
    
    public Socket getSocket() {
        return socket;
    }
    
    public List<String> getLst() {
        return lst;
    }
    
    // Setter methods
    public void setClientaddr(String clientaddr) {
        this.clientaddr = clientaddr;
    }
    
    public void setClientport(int clientport) {
        this.clientport = clientport;
    }
    
    public void setSocket(Socket socket) {
        this.socket = socket;
    }
    
    public void setLst(List<String> lst) {
        this.lst = lst;
    }
    
    
}
