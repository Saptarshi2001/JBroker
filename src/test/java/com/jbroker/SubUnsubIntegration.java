
package com.jbroker;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.Socket;

public class SubUnsubIntegration {


static Thread serverThread;

@BeforeAll
static void startServer() throws Exception {

    serverThread = new Thread(() -> {
        try {
            Server server = new Server();
            server.connect("localhost:4222");
        } catch (Exception e) {
            e.printStackTrace();
        }
    });

    serverThread.setDaemon(true);
    serverThread.start();

    Thread.sleep(500);
}

private BufferedReader reader;
private BufferedWriter writer;
private Socket socket;

@BeforeEach
void connectClient() throws Exception {

    socket = new Socket("localhost", 4222);

    reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));

    writer = new BufferedWriter(
            new OutputStreamWriter(socket.getOutputStream()));

    // consume server banner
    for(int i=0;i<6;i++)
        reader.readLine();

    writer.write("Connect {}\n");
    writer.flush();

    assertEquals("+OK", reader.readLine());
}

@AfterEach
void closeClient() throws Exception {
    socket.close();
}

// -------- SUB TEST --------

@Test
void testValidSub() throws Exception {

    writer.write("Sub foo 1\n");
    writer.flush();

    String response = reader.readLine();
    assertEquals("Subscribed", response);
}

@Test
void testDuplicateSubId() throws Exception {

    writer.write("Sub foo 1\n");
    writer.flush();
    reader.readLine();

    writer.write("Sub bar 1\n");
    writer.flush();

    String response = reader.readLine();
    assertEquals("Id already present", response);
}

// -------- UNSUB TEST --------

@Test
void testValidUnsub() throws Exception {

    writer.write("Sub foo 1\n");
    writer.flush();
    reader.readLine();

    writer.write("Unsub 1\n");
    writer.flush();

    String response = reader.readLine();
    assertEquals("Unsubbed", response);
}

@Test
void testUnsubNonExistingId() throws Exception {

    writer.write("Unsub 99\n");
    writer.flush();

    String response = reader.readLine();
    assertEquals("Subscription not found",response);
}

// -------- MULTIPLE SUBSCRIPTIONS --------

@Test
void testMultipleSubscriptionsUnsubOne() throws Exception {

    writer.write("Sub foo 1\n");
    writer.flush();
    reader.readLine();

    writer.write("Sub foo 2\n");
    writer.flush();
    reader.readLine();

    writer.write("Unsub 1\n");
    writer.flush();

    String response = reader.readLine();
    assertEquals("Unsubbed", response);
}







}
