package com.jewel_system.link_server;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by Benjamin Claassen <BClaassen@live.com> on 8/6/2016.
 *
 * @author <a href="mailto:BClaassen@live.com">Benjamin Claassen</a>
 */
public class Configuration {
    public static HttpServer SERVER;
    public static InetAddress ADDRESS;
    public static int PORT;
    public static String PROTOCOL = "http";
    public static String WEB_BASE = new File("web").getAbsolutePath();
    public static String ERROR_404 = "404";

    static {
        if (Files.exists(Paths.get(WEB_BASE + "/404.html"))) {
            try {
                ERROR_404 = new String(Files.readAllBytes(Paths.get(WEB_BASE + "/404.html")), Charset.forName("UTF-8"));
            } catch (IOException e) {
                System.out.println("CANNOT READ " + WEB_BASE + "/404.html");
                e.printStackTrace();
            }
        }
    }
}
