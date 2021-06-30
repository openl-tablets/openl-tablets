package org.openl.itest.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

class HttpData {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern NO_CONTENT_STATUS_PATTERN = Pattern.compile("HTTP/\\S+\\s+204(\\s.*)?");

    private final String firstLine;
    private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final byte[] body;
    private final String resource;
    private String cookie;

    private HttpData(String firstLine, Map<String, String> headers, byte[] body, String resource) {
        this.firstLine = firstLine;
        this.resource = resource;
        this.headers.putAll(headers);
        this.body = body;
    }

    private int getResponseCode() {
        if (firstLine == null) {
            return 200; // OK
        }
        String[] status = firstLine.split(" ", 3);
        return Integer.parseInt(status[1]);
    }

    private String getHttpMethod() {
        String[] status = firstLine.split(" ", 3);
        return status[0];
    }

    private String getUrl() {
        String[] status = firstLine.split(" ", 3);
        return status[1];
    }

    static HttpData readFile(String resource) throws IOException {
        try (InputStream input = HttpData.class.getResourceAsStream(resource)) {
            return readData(input, resource);
        }
    }

    static HttpData send(URL baseURL, String resource, String sessionId) throws IOException {
        HttpData httpData = readFile(resource);
        if (httpData == null) {
            throw new FileNotFoundException(resource);
        }
        HttpURLConnection connection = openConnection(URI.create(baseURL.toString() + httpData.getUrl()).toURL(),
            httpData.getHttpMethod(), sessionId);
        write(connection, httpData);
        return readData(connection, resource);
    }

    private static HttpURLConnection openConnection(URL url, String httpMethod, String cookie) throws IOException {
        URLConnection connection = url.openConnection();
        if (StringUtils.hasLength(cookie)) {
            connection.setRequestProperty("Cookie", cookie);
        }
        if (!(connection instanceof HttpURLConnection)) {
            throw new IllegalStateException("HttpURLConnection required for [" + url + "] but got: " + connection);
        }
        connection.setConnectTimeout(Integer.parseInt(System.getProperty("http.timeout.connect")));
        connection.setReadTimeout(Integer.parseInt(System.getProperty("http.timeout.read")));
        connection.setDoInput(true);
        if ("GET".equals(httpMethod)) {
            ((HttpURLConnection) connection).setInstanceFollowRedirects(true);
        } else {
            ((HttpURLConnection) connection).setInstanceFollowRedirects(false);
        }
        if ("POST".equals(httpMethod) || "PUT".equals(httpMethod) || "DELETE".equals(httpMethod) || "PATCH"
            .equals(httpMethod)) {
            connection.setDoOutput(true);
        } else {
            connection.setDoOutput(false);
        }
        ((HttpURLConnection) connection).setRequestMethod(httpMethod);
        return (HttpURLConnection) connection;
    }

    private static void write(HttpURLConnection connection, HttpData httpData) throws IOException {
        httpData.headers.putIfAbsent("Content-Length", String.valueOf(httpData.body.length));
        httpData.headers.putIfAbsent("Host", "example.com");
        httpData.headers.forEach(connection::addRequestProperty);
        connection.connect();
        if (httpData.body.length != 0) {
            try (OutputStream os = connection.getOutputStream()) {
                os.write(httpData.body);
            }
        }
    }

    void assertTo(HttpData expected) {
        try {
            if (expected == null) {
                assertEquals("Status code: ", 200, this.getResponseCode());
                return;
            }
            assertEquals("Status code: ", expected.getResponseCode(), this.getResponseCode());
            for (Map.Entry<String, String> r : expected.headers.entrySet()) {
                String headerName = r.getKey();
                String value = r.getValue();
                assertEquals(headerName, value, this.headers.get(headerName));
            }

            if (expected.body == null) {
                return; // No body expected
            }
            String contentType = headers.get("Content-Type");
            contentType = contentType == null ? "null" : contentType;
            int sep = contentType.indexOf(';');
            if (sep > 0) {
                String encoding = contentType.substring(sep + 1);
                contentType = contentType.substring(0, sep);
            }
            switch (contentType) {
                case "text/html":
                case "text/plain":
                    Comparators.txt("Difference", expected.body, this.body);
                    break;
                case "application/xml":
                case "text/xml":
                    Comparators.xml("Difference", expected.body, this.body);
                    break;
                case "application/json":
                    JsonNode actualNode;
                    actualNode = OBJECT_MAPPER.readTree(this.body);
                    JsonNode expectedNode = OBJECT_MAPPER.readTree(expected.body);
                    Comparators.compareJsonObjects(expectedNode, actualNode, "");
                    break;
                default:
                    assertArrayEquals("Body: ", expected.body, this.body);
            }
        } catch (Exception | AssertionError ex) {
            log(expected != null ? expected.resource : resource, firstLine, headers, body);
            throw new RuntimeException(ex);
        }
    }

    static void log(String resourceName, String firstLine, Map<String, String> headers, byte[] body) {
        try {
            System.err.println("--------------------");
            System.err.println(firstLine);
            for (Map.Entry<String, String> r : headers.entrySet()) {
                String headerName = r.getKey();
                String value = r.getValue();
                System.err.println(headerName + ": " + value);
            }
            System.err.println();

            StreamUtils.copy(body, System.err);
            System.err.println("\n--------------------");

            String path = System.getProperty("server.responses") + resourceName + ".body";
            Path responsePath = Paths.get(path);
            Files.createDirectories(responsePath.getParent());
            Files.write(responsePath, body);
        } catch (IOException ignored) {
            // Ignored
        }
    }

    private static HttpData readData(HttpURLConnection connection, String resource) throws IOException {
        connection.getResponseCode();
        InputStream input = connection.getErrorStream();
        if (input == null) {
            try {
                input = connection.getInputStream();
            } catch (IOException ignored) {

            }
        }
        try {
            String firstLine = connection.getHeaderField(0);
            String cookie = "";
            Map<String, String> headers = new HashMap<>();
            for (Map.Entry<String, List<String>> entries : connection.getHeaderFields().entrySet()) {
                if (entries.getKey() != null) {
                    if (entries.getKey().equals("Set-Cookie")) {
                        cookie = String.join("; ", entries.getValue());
                    }
                    headers.put(entries.getKey(), String.join(", ", entries.getValue()));
                }
            }

            byte[] body = input != null ? StreamUtils.copyToByteArray(input) : new byte[0];
            HttpData httpData = new HttpData(firstLine, headers, body, resource);
            httpData.setCookie(cookie);
            return httpData;
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    private static HttpData readData(InputStream input, String resource) throws IOException {
        if (input == null) {
            return null;
        }
        String firstLine = readLine(input);
        Map<String, String> headers = readHeaders(input);

        byte[] body;
        String cl = headers.get("Content-Length");
        String te = headers.get("Transfer-Encoding");
        String ct = headers.get("Content-Type");

        if (ct != null && ct.startsWith("multipart/form-data") && ct.contains("boundary=")) {
            String boundary = ct.substring(ct.indexOf("boundary=") + "boundary=".length());
            String boundaryEnd = "--" + boundary + "--";
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try (PrintWriter writer = new PrintWriter(os)) {
                while (true) {
                    String line = readLine(input);
                    if (!line.isEmpty() && line.charAt(0) == '&') {
                        writer.flush();
                        String fileRes = Paths.get(resource)
                                .getParent()
                                .resolve(line.substring(1))
                                .toString().replace('\\', '/');
                        try (InputStream fileStream = HttpData.class.getResourceAsStream(fileRes)) {
                            StreamUtils.copy(fileStream, os);
                        }
                        os.flush();
                    } else {
                        writer.append(line);
                    }
                    writer.print("\r\n");
                    if (boundaryEnd.equals(line)) {
                        writer.flush();
                        break;
                    }
                }
                writer.print("\r\n");
            }
            body = os.toByteArray();
        } else if (cl != null) {
            body = readBody(input, cl);
        } else if (te != null && te.equalsIgnoreCase("chunked")) {
            body = readChunckedBody(input);
        } else if (NO_CONTENT_STATUS_PATTERN.matcher(firstLine).matches()) {
            // Depending on the implementation of InputStream, reading it can hang if no data is available.
            // So for 204 status we just don't read body because it doesn't needed for this status.
            body = new byte[0];
        } else {
            body = StreamUtils.copyToByteArray(input);
        }

        return new HttpData(firstLine, headers, body, resource);
    }

    private static Map<String, String> readHeaders(InputStream input) throws IOException {
        TreeMap<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        String header = readLine(input);
        while (!header.isEmpty()) {
            int separator = header.indexOf(":");
            String name = header.substring(0, separator);
            String value = header.substring(separator + 1).trim();
            value = value.isEmpty() ? null : value;
            headers.put(name, value);
            header = readLine(input);
        }
        return headers;
    }

    private static byte[] readBody(InputStream input, String length) throws IOException {
        byte[] body;
        int size = Integer.parseInt(length);
        body = new byte[size];
        int actual = input.read(body);
        if (actual != size) {
            throw new IOException("Unexpected size of the body.");
        }
        return body;
    }

    private static byte[] readChunckedBody(InputStream input) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream(1024);
        byte[] chunk = readChunck(input);

        while (chunk.length > 0) {
            body.write(chunk);
            chunk = readChunck(input);
        }
        return body.toByteArray();
    }

    private static byte[] readChunck(InputStream input) throws IOException {
        String hexSize = readLine(input);
        int size = Integer.parseInt(hexSize, 16);
        byte[] body = new byte[size];
        int actual = input.read(body);
        if (actual != size) {
            throw new IOException("Unexpected size of the chunk.");
        }
        String eol = readLine(input);
        if (eol.isEmpty()) {
            return body;
        }
        throw new IOException("Unexpected format of the chunk.");
    }

    private static String readLine(InputStream input) throws IOException {
        StringBuilder line = new StringBuilder(120);
        boolean eol = false;
        int n;
        while (!eol && (n = input.read()) > 0) {

            if (n != 10 && n != 13) {
                line.append((char) n);
            } else {
                eol = n == 10;
            }
        }
        if (!eol) {
            throw new IOException("Unexpected end of the stream.");
        }
        return line.toString();
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }
}
