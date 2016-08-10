package com.jewel_system.link_server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.activation.MimetypesFileTypeMap;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Benjamin Claassen <BClaassen@live.com> on 8/5/2016.
 *
 * @author <a href="mailto:BClaassen@live.com">Benjamin Claassen</a>
 */
public class Start {
    public static void main(String[] args) {
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
            try {
                Path file = Paths.get(Configuration.WEB_BASE + httpExchange.getRequestURI().getRawPath()).toAbsolutePath();
                if (Files.isDirectory(file)) {
                    file = Paths.get(file.toString() + "/index.html");
                }
                //"text/html"
                String extension = "",
                       fileName = file.toString();

                int i = fileName.lastIndexOf('.');
                int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

                if (i > p) {
                    extension = fileName.substring(i+1);
                }

                String mime;
                switch (extension) {
                    case "html":
                    case "htm":
                    case "php":
                        mime = "text/html";
                        break;
                    case "css":
                        mime = "text/css";
                        break;

                    default:
                        mime = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(file.toString());
                }

                httpExchange.getResponseHeaders().add("content-type", mime);
                if (Files.exists(file)) {
                    httpExchange.sendResponseHeaders(200, Files.size(file));
                    Files.copy(file, httpExchange.getResponseBody());
                    httpExchange.close();
                } else {
                    write404(httpExchange);
                }
            } catch (Exception e) {
                e.printStackTrace();
                httpExchange.sendResponseHeaders(500, 0);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                httpExchange.getResponseBody().write(sw.toString().getBytes("UTF-8"));
            }
            httpExchange.close();
        });
        Configuration.SERVER.createContext("/api/", httpExchange -> {
            try {
                URL url = new URL(Configuration.PROTOCOL + "://" + Configuration.ADDRESS.getHostAddress() + ":" + Configuration.PORT + httpExchange.getRequestURI());

                URLConnection cc = url.openConnection();
                if (cc instanceof HttpURLConnection) {

                    BackendRequest request = new BackendRequest(httpExchange.getRequestHeaders(), httpExchange.getRequestBody(), httpExchange.getRequestMethod(), httpExchange.getRequestURI().toString());
                    //TODO: Store if sending fails
                    request.sendRequest(httpExchange);
                }
                httpExchange.close();
            } catch (IOException e) {
                e.printStackTrace();
                httpExchange.sendResponseHeaders(500, 0);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                httpExchange.getResponseBody().write(sw.toString().getBytes("UTF-8"));
                httpExchange.close();
            }
        });
    }

    private static void write404(HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(404, Configuration.ERROR_404.length());
        httpExchange.getResponseBody().write(Configuration.ERROR_404.getBytes("UTF-8"));
    }

    /**
     * Start the server
     */
    public static void startServer() {
        Configuration.SERVER.start();
    }
}
