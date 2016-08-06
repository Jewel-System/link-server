package com.jewel_system.link_server;

import com.sun.net.httpserver.HttpServer;

import java.net.InetAddress;

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
}
