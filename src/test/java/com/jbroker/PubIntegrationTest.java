package com.jbroker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PubIntegrationTest {

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

        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            try (Socket testSocket = new Socket()) {
                testSocket.connect(new java.net.InetSocketAddress("localhost", 4222), 1000);
                return;
            } catch (IOException e) {
                Thread.sleep(200);
            }
        }
        throw new RuntimeException("Server failed to start on port 4222");
    }

    private void consumeBanner(BufferedReader r) throws IOException {
        String line;
        while ((line = r.readLine()) != null) {
            if (line.contains("Port")) break;
        }
    }

    private String readNonEmptyLine(BufferedReader r) throws IOException {
        String line;
        while ((line = r.readLine()) != null) {
            if (!line.trim().isEmpty()) return line;
        }
        return null;
    }

    private void send(BufferedWriter w, String msg) throws IOException {
        w.write(msg + "\n");
        w.flush();
    }

    @Test
    void testPubDeliversToSubscriber() throws Exception {
        Socket subSock = new Socket();
        subSock.connect(new java.net.InetSocketAddress("localhost", 4222));
        subSock.setSoTimeout(5000);
        BufferedReader subR = new BufferedReader(new InputStreamReader(subSock.getInputStream()));
        BufferedWriter subW = new BufferedWriter(new OutputStreamWriter(subSock.getOutputStream()));
        consumeBanner(subR);
        send(subW, "Connect {}");
        assertEquals("+OK", readNonEmptyLine(subR));
        send(subW, "Sub mytopic 1");
        assertEquals("Subscribed", subR.readLine());

        Socket pubSock = new Socket();
        pubSock.connect(new java.net.InetSocketAddress("localhost", 4222));
        pubSock.setSoTimeout(5000);
        BufferedReader pubR = new BufferedReader(new InputStreamReader(pubSock.getInputStream()));
        BufferedWriter pubW = new BufferedWriter(new OutputStreamWriter(pubSock.getOutputStream()));
        consumeBanner(pubR);
        send(pubW, "Connect {}");
        assertEquals("+OK", readNonEmptyLine(pubR));

        String payload = "hello-pub";
        send(pubW, "Pub mytopic " + payload.length());
        send(pubW, payload);

        String received = subR.readLine();
        assertEquals(payload, received);

        subSock.close();
        pubSock.close();
    }

    @Test
    void testPublishInvalidFormat() throws Exception {
        Socket pubSock = new Socket();
        pubSock.connect(new java.net.InetSocketAddress("localhost", 4222));
        pubSock.setSoTimeout(5000);
        BufferedReader pubR = new BufferedReader(new InputStreamReader(pubSock.getInputStream()));
        BufferedWriter pubW = new BufferedWriter(new OutputStreamWriter(pubSock.getOutputStream()));
        consumeBanner(pubR);
        send(pubW, "Connect {}");
        assertEquals("+OK", readNonEmptyLine(pubR));

        send(pubW, "Pub mytopic abc");

        String response = pubR.readLine();

        assertEquals("Invalid pub message !!! Please type Pub [Topic_name][message_length]", response);

        pubSock.close();
    }

    @Test
    void testPublishNoTopic() throws Exception {
        Socket client = new Socket();
        client.connect(new java.net.InetSocketAddress("localhost", 4222));
        client.setSoTimeout(3000);
        BufferedReader r = new BufferedReader(new InputStreamReader(client.getInputStream()));
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
        consumeBanner(r);
        send(w, "Connect {}");
        assertEquals("+OK", readNonEmptyLine(r));

        send(w, "Pub unknowntopic 4");
        send(w, "John");

        try {
            String response = r.readLine();
            assertEquals("No topic found", response);
        } catch (java.net.SocketTimeoutException e) {
        }

        client.close();
    }
}
