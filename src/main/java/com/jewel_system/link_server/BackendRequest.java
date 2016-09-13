package com.jewel_system.link_server;

import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Benjamin Claassen <BClaassen@live.com> on 8/7/2016.
 *
 * @author <a href="mailto:BClaassen@live.com">Benjamin Claassen</a>
 */
public class BackendRequest implements Serializable {

    public static final long serialVersionUID = 3487495895819393L;

    private HashMap<String, List<String>> headers = new HashMap<>(32);
    private byte[] content;
    private String method;
    private String location;

    public BackendRequest() {
    }

    public BackendRequest(Map<String, List<String>> headers, InputStream content, String method, String location) throws IOException {
        this.headers.putAll(headers);
        this.method = method;
        this.location = location;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = content.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        this.content = buffer.toByteArray();
    }

    public BackendRequest(Map<String, List<String>> headers, byte[] content, String method, String location) {
        this.headers.putAll(headers);
        this.content = content;
        this.method = method;
        this.location = location;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers.clear();
        this.headers.putAll(headers);
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public void sendRequest(HttpExchange exchange) throws IOException {
        URL url = new URL(Configuration.PROTOCOL + "://" + Configuration.ADDRESS.getHostName() + ":" + Configuration.PORT + location);

        URLConnection cc = url.openConnection();
        if (cc instanceof HttpURLConnection) {

            HttpURLConnection connection = (HttpURLConnection) cc;

            connection.setDoInput(true);
            headers.forEach((key, value) -> {
                if (key != null) {
                    for (String v : value) {
                        connection.addRequestProperty(key, v);
                    }
                }
            });

            if (content.length > 0) {
                connection.setDoOutput(true);
            }

            connection.setRequestMethod(method);

            if (content.length > 0) {
                OutputStream os = connection.getOutputStream();
                os.write(content);
                os.flush();
            }

            connection.connect();

            if (exchange != null) {
                connection.getHeaderFields().forEach((k, v) -> {
                    if (k != null) {
                        exchange.getResponseHeaders().put(k, v);
                    }
                });
                if (connection.getResponseCode() == 304) {
                    exchange.sendResponseHeaders(connection.getResponseCode(), -1);
                } else {
                    exchange.sendResponseHeaders(connection.getResponseCode(), 0);

                    InputStream is;
                    if (connection.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                        is = connection.getErrorStream();
                    } else {
                        is = connection.getInputStream();
                    }

                    byte[] data = new byte[16384];
                    int nRead;

                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                        exchange.getResponseBody().write(data, 0, nRead);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "BackendRequest{" +
                "headers=" + headers +
                ", content=" + Arrays.toString(content) +
                ", method='" + method + '\'' +
                '}';
    }
}
