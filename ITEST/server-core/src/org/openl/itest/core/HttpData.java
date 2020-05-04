package org.openl.itest.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class HttpData {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String firstLine;
    private final Map<String, String> headers = new TreeMap<>(String::compareToIgnoreCase);
    private final byte[] body;
    private final String resource;

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
        return Integer.valueOf(status[1]);
    }

    static HttpData readFile(String resource) throws IOException {
        try (InputStream input = HttpData.class.getResourceAsStream(resource)) {
            return readData(input, resource);
        }
    }

    static HttpData send(URL baseURL, String resource) throws IOException {
        try (Socket socket = new Socket()) {

            int readTimeout = Integer.parseInt(System.getProperty("http.timeout.read"));
            int connectTimeout = Integer.parseInt(System.getProperty("http.timeout.connect"));

            socket.setSoTimeout(readTimeout);
            socket.connect(new InetSocketAddress(baseURL.getHost(), baseURL.getPort()), connectTimeout);
            OutputStream outputStream = socket.getOutputStream();
            write(outputStream, resource);
            InputStream input = socket.getInputStream();

            return readData(input, resource);
        }
    }

    private static void write(OutputStream output, String resource) throws IOException {
        HttpData httpData = readFile(resource);

        httpData.headers.putIfAbsent("Content-Length", String.valueOf(httpData.body.length));
        httpData.headers.putIfAbsent("Host", "example.com");
        OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.ISO_8859_1);
        writer.append(httpData.firstLine).append('\r').append('\n');
        for (Map.Entry<String, String> e : httpData.headers.entrySet()) {
            writer.append(e.getKey()).append(": ").append(e.getValue()).append('\r').append('\n');
        }
        writer.append('\r').append('\n');
        writer.flush();
        output.write(httpData.body);
        output.flush();
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
                    JsonNode actualNode = null;
                    actualNode = OBJECT_MAPPER.readTree(this.body);
                    JsonNode expectedNode = OBJECT_MAPPER.readTree(expected.body);
                    Comparators.compareJsonObjects(expectedNode, actualNode, "");
                    break;
                default:
                    assertArrayEquals("Body: ", expected.body, this.body);
            }
        } catch (Exception | AssertionError ex) {
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

                String path = System.getProperty("server.responses") + expected.resource + ".body";
                Path responsePath = Paths.get(path);
                Files.createDirectories(responsePath.getParent());
                Files.write(responsePath, body);
            } catch (IOException ignored) {
                // Ignored
            }
            throw new RuntimeException(ex);
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
        if (cl != null) {
            body = readBody(input, cl);
        } else if (te != null && te.equalsIgnoreCase("chunked")) {
            body = readChunckedBody(input);
        } else {
            body = StreamUtils.copyToByteArray(input);
        }

        return new HttpData(firstLine, headers, body, resource);
    }

    private static Map<String, String> readHeaders(InputStream input) throws IOException {
        TreeMap<String, String> headers = new TreeMap<>(String::compareToIgnoreCase);

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
}
