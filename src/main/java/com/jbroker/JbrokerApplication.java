package com.jbroker;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class JbrokerApplication {

    public static void main(String[] args) throws IOException, InterruptedException {

        Server broker = new Server();

        Properties props = new Properties();
        try (InputStream in = JbrokerApplication.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                props.load(in);
            }
        }

        String host = props.getProperty("server.address", System.getenv().getOrDefault("SERVER_ADDRESS", "127.0.0.1"));
        String port = props.getProperty("server.port", System.getenv().getOrDefault("SERVER_PORT", "4222"));

        String address = host + ":" + port;
        broker.connect(address);
    }

}
