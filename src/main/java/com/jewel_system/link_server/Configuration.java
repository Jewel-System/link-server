package com.jewel_system.link_server;

import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Benjamin Claassen <BClaassen@live.com> on 8/6/2016.
 *
 * @author <a href="mailto:BClaassen@live.com">Benjamin Claassen</a>
 */
public class Configuration {
    public static HttpServer SERVER;

    public static String PROTOCOL = "http";
    public static InetAddress ADDRESS;
    public static int PORT = 53455;

    public static ConcurrentHashMap<String, String> ERRORS = new ConcurrentHashMap<>();

    public static String TYPE = "late";

    public static String WEB_BASE = Paths.get("web").toAbsolutePath().toString();

    static {
        try {
            fromJson(new String(Files.readAllBytes(Paths.get("config.json")), Charset.forName("UTF-8")));
        } catch (IOException ignored) {
        }
        try {
            ADDRESS = InetAddress.getByName("steve.zanity.net");
        } catch (Exception ignored) {
        }

        if (Files.exists(Paths.get(WEB_BASE + "/404.html"))) {
            ERRORS.put("404", Paths.get(WEB_BASE + "/404.html").toAbsolutePath().toString());
        }
    }

    private Configuration() {
    }

    public static String toJson() {
        String result = "{\n" +
                "    \"server\": {\n" +
                "        \"protocol\":\"" + PROTOCOL + "\",\n" +
                "        \"host\":\"" + ADDRESS.getHostName() + "\",\n" +
                "        \"port\":\"" + PORT + "\"\n" +
                "    },\n" +
                "   \"errors\": {\n";

        for (Map.Entry<String, String> entry : ERRORS.entrySet()) {
            result += "        \"" + entry.getKey() + "\":\"" + entry.getValue() + "\",\n";
        }
        result += "    },\n" +
                "    \"machine\": {\n" +
                "        \"type\":\"" + TYPE + "\"\n" +
                "    }\n" +
                "}";

        return result;
    }

    public static void fromJson(String jsons) throws UnknownHostException {
        JSONObject json = jsons != null ? new JSONObject(jsons) : null;
        if (json == null) {
            throw new IllegalStateException("JSON is required");
        }
        if (json.has("server")) {
            JSONObject server = json.getJSONObject("server");
            if (json.has("host")) {
                ADDRESS = InetAddress.getByName(server.getString("host"));
            }
            if (json.has("protocol")) {
                PROTOCOL = server.getString("protocol");
            }

            if (json.has("port")) {
                PORT = server.getInt("port");
            }
        }
        if (json.has("errors")) {
            JSONObject errors = json.getJSONObject("errors");
            errors.keySet().forEach(k -> ERRORS.put(k, errors.getString(k)));
        }
        if (json.has("machine")) {
            JSONObject machine = json.getJSONObject("machine");
            if (machine.has("type")) {
                TYPE = machine.getString("type");
            }
        }
    }

    public static void store() throws IOException {
        Files.write(Paths.get("config.json"), toJson().getBytes("UTF-8"));
    }
}
