package com.jewel_system.link_server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.activation.MimetypesFileTypeMap;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Scanner;

/**
 * Created by Benjamin Claassen <BClaassen@live.com> on 8/5/2016.
 *
 * @author <a href="mailto:BClaassen@live.com">Benjamin Claassen</a>
 */
public class Start {
    private static volatile Connection conn;

    public static void main(String[] args) {
        initializeDB();
        setupOldRequestProcessor();
        createServer();
        setupHandlers();
        startServer();
        findServer();
    }

    /**
     * Setup handler to process old requests to the api
     */
    private static void setupOldRequestProcessor() {
        new Thread(() -> {
            while (true) {
                try {
                    ResultSet resultSet = conn.createStatement().executeQuery("SELECT (SELECT count(*) FROM Store) as count, * FROM Store ORDER BY id ASC");

                    boolean flip = true;
                    while (resultSet.next()) {
                        if (flip) {
                            flip = false;
                            System.out.println(resultSet.getInt(1) + " request(s) are in waiting and are being processed");
                        }
                        try {
                            byte[] buf = resultSet.getBytes(3);
                            ObjectInputStream objectIn = null;
                            if (buf != null) {
                                objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
                            }

                            if (objectIn != null) {
                                ((BackendRequest) objectIn.readObject()).sendRequest(null);
                            }

                            PreparedStatement statement = conn.prepareStatement("DELETE FROM Store WHERE id = ?");
                            statement.setInt(1, resultSet.getInt(2));
                            statement.execute();
                        } catch (IOException | ClassNotFoundException ignored) {
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(5 * 100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "Old Request Processor").start();
    }

    /**
     * Connects to a SQLite database to store requests
     */
    private static void initializeDB() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:store.db");
            conn.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS Store (id INTEGER PRIMARY KEY AUTOINCREMENT, object blob);");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Could not setup database, shutting down");
            System.exit(1);
        }
    }

    /**
     * Find the backend server on the network
     */
    public static void findServer() {
        try {
            System.out.println("Finding Server");
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
            System.out.println("Found server at: " + Configuration.ADDRESS.getHostName() + ":" + Configuration.PORT);
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
            JOptionPane.showMessageDialog(null, "Could not create server, shutting down");
            System.exit(1);
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
                   Path filed = Paths.get(file.toString() + "/index.html");
                    if (!Files.exists(file)) {
                        file = Paths.get(file.toString() + "/index.html");
                    } else {
                        file = filed;
                    }
                }
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
                    httpExchange.sendResponseHeaders(200, 0);
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
        Configuration.SERVER.createContext("/link/config", httpExchange -> {
            try {
                if ("POST".equalsIgnoreCase(httpExchange.getRequestMethod())) {
                    try (Scanner scanner = new Scanner(httpExchange.getRequestBody()).useDelimiter("\\A")) {
                        Configuration.fromJson(scanner.hasNext() ? scanner.next() : null);
                        Configuration.store();
                    }
                }
                httpExchange.sendResponseHeaders(200, 0);
                httpExchange.getResponseBody().write(Configuration.toJson().getBytes("UTF-8"));
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
        Configuration.SERVER.createContext("/api/", httpExchange -> {
            try {
                URL url = new URL(Configuration.PROTOCOL + "://" + Configuration.ADDRESS.getHostAddress() + ":" + Configuration.PORT + httpExchange.getRequestURI());

                URLConnection cc = url.openConnection();
                if (cc instanceof HttpURLConnection) {

                    BackendRequest request = new BackendRequest(httpExchange.getRequestHeaders(), httpExchange.getRequestBody(), httpExchange.getRequestMethod(), httpExchange.getRequestURI().toString());
                    try {
                        request.sendRequest(httpExchange);
                    } catch (IOException ex) {
                        try {
                            PreparedStatement statement = conn.prepareStatement("INSERT INTO Store (object) VALUES (?)");

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ObjectOutputStream oos = new ObjectOutputStream(baos);
                            oos.writeObject(request);
                            oos.flush();
                            oos.close();

                            statement.setBytes(1, baos.toByteArray());
                            statement.execute();
                        } catch (SQLException e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(null, "Could not store request, shutting down");
                            System.exit(1);
                        }
                        throw ex;
                    }
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
        httpExchange.sendResponseHeaders(404, 0);
        Files.copy(Paths.get(Configuration.ERRORS.get("404")), httpExchange.getResponseBody());
    }

    /**
     * Start the server
     */
    public static void startServer() {
        Configuration.SERVER.start();
    }
}
