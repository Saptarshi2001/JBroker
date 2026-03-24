package com.jbroker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.Socket;

public class IntegrationTest {


@Test
void testConnectCommand() throws Exception {

    // Start server in a separate thread
    Thread serverThread = new Thread(() -> {
        try {
            Server server = new Server();
            server.connect("localhost:4222");
        } catch (Exception e) {
            e.printStackTrace();
        }
    });

    serverThread.setDaemon(true);
    serverThread.start();

    // Give server time to start
    Thread.sleep(500);

    // Connect as client
    Socket socket = new Socket("localhost", 4222);

    BufferedReader reader =
            new BufferedReader(new InputStreamReader(socket.getInputStream()));

    BufferedWriter writer =
            new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

    // Consume banner lines sent by server
    reader.readLine(); // Connected to client
    reader.readLine(); // -------------------
    reader.readLine(); // Server Info
    reader.readLine(); // Host : ...
    reader.readLine(); // Port : ...
    reader.readLine();
    // Send CONNECT command
    writer.write("Connect {}\n");
    writer.flush();

    // Read server response
    String response = reader.readLine();

    assertEquals("+OK", response);

    socket.close();
}


}
