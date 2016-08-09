package com.jewel_system.link_server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.*;

/**
 * Created by Benjamin Claassen <BClaassen@live.com> on 8/5/2016.
 *
 * @author <a href="mailto:BClaassen@live.com">Benjamin Claassen</a>
 */
public class Start {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();

                System.exit(1);
            }
        });

        findServer();
        createServer();
        setupHandlers();
        startServer();
    }

    /**
     * Find the backend server on the network
     */
    public static void findServer() {
        try {
            DatagramSocket socket = new DatagramSocket(53456);
            byte[] test = {1};
            socket.send(new DatagramPacket(test, test.length, InetAddress.getByName("224.0.0.1"), 53457));
            socket.send(new DatagramPacket(test, test.length, InetAddress.getByName("10.0.0.255"), 53457));
            socket.send(new DatagramPacket(test, test.length, InetAddress.getByName("192.168.0.255"), 53457));
            socket.send(new DatagramPacket(test, test.length, InetAddress.getByName("172.16.0.255"), 53457));
            socket.send(new DatagramPacket(test, test.length, InetAddress.getByName("255.255.255.255"), 53457));
            test = new byte[32];
            DatagramPacket packet = new DatagramPacket(test, test.length);
            socket.receive(packet);
            Configuration.ADDRESS = packet.getAddress();
            Configuration.PORT = Integer.parseInt(new String(test).trim());
            socket.close();
            System.out.println("Found server at: " + Configuration.ADDRESS + ":" + Configuration.PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Launch the local server
     */
    public static void createServer() {
        try {
            Configuration.SERVER = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 200), 50);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Create handlers for processing http requests
     */
    public static void setupHandlers() {
        Configuration.SERVER.createContext("/", httpExchange -> {
            //TODO: Process local request
            System.out.println("Connection");
        });
        Configuration.SERVER.createContext("/api/", httpExchange -> {
            //TODO: Send to backend
            try {
                URL url = new URL(Configuration.PROTOCOL + "://" + Configuration.ADDRESS.getHostAddress() + ":" + Configuration.PORT + httpExchange.getRequestURI());

                URLConnection cc = url.openConnection();
                if (cc instanceof HttpURLConnection) {

                    BackendRequest request = new BackendRequest(httpExchange.getRequestHeaders(), httpExchange.getRequestBody(), httpExchange.getRequestMethod(), httpExchange.getRequestURI().toString());
                    request.sendRequest(httpExchange);
                }
                httpExchange.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Start the server
     */
    public static void startServer() {
        Configuration.SERVER.start();
    }
}
