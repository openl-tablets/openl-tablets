package org.openl.itest.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

class HttpData {
    static final ObjectMapper OBJECT_MAPPER;
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_ENCODING_HEADER = "Content-Encoding";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(.*?)}");
    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{(.*?)}");
    private static final Pattern CHARSET_PATTERN = Pattern.compile(
            "(?:^|;)\\s*charset\\s*=\\s*(?:\"([^\"]+)\"|([^;\\s]+))",
            Pattern.CASE_INSENSITIVE);

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static final Pattern NO_CONTENT_STATUS_PATTERN = Pattern.compile("HTTP/\\S+\\s+204(\\s.*)?");
    private static final Set<String> BLOB_TYPES = Stream.of("application/zip").collect(Collectors.toSet());
    private static final Set<String> TEXT_BODY_TYPES = Set.of(
            "application/json",
            "application/xml",
            "application/x-www-form-urlencoded");

    private final String firstLine;
    private final TreeMap<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, String> settings = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final byte[] body;
    private final String pathToResource;
    private String cookie;

    private HttpData(String firstLine, Map<String, String> headers, byte[] body, String pathToResource) {
        this.firstLine = firstLine;
        this.headers.putAll(headers);
        var settings = this.headers.subMap("X-OpenL-Test-", "X-OpenL-Test.");
        this.settings.putAll(settings);
        settings.clear();
        this.body = body;
        this.pathToResource = pathToResource;
    }

    String getSetting(String key) {
        return settings.get("X-OpenL-Test-" + key);
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
        try (InputStream input = getStream(resource)) {
            return readData(input, resource);
        }
    }

    static HttpData ok() {
        return new HttpData("HTTP/1.1 200 OK", Collections.emptyMap(), null, null);
    }

    static HttpData send(URI baseURL, HttpData httpData, String cookie, Map<String, String> localEnv) {
        String url = resolvePathVariables(httpData.getUrl(), localEnv);
        var request = HttpRequest.newBuilder()
                .uri(URI.create(baseURL.toString() + url))
                .method(httpData.getHttpMethod(),
                        HttpRequest.BodyPublishers.ofByteArray(httpData.resolveBodyPlaceholders(localEnv)))
                .timeout(Duration.ofMillis(Integer.parseInt(System.getProperty("http.timeout.read"))))
                .header("Host", "example.com");
        httpData.headers.forEach((key, value) -> request.header(key, replacePlaceholders(value, localEnv)));

        if (cookie != null && !cookie.isEmpty() && !httpData.headers.containsKey("Cookie")) {
            request.header("Cookie", cookie);
        }

        var response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Integer.parseInt(System.getProperty("http.timeout.connect"))))
                .build()
                .sendAsync(request.build(), HttpResponse.BodyHandlers.ofByteArray()).join();
        return readData(response);
    }

    private static String replacePlaceholders(String text, Map<String, String> env) {
        var matcher = PLACEHOLDER_PATTERN.matcher(text);

        var result = new StringBuilder();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = env.get(placeholder);
            if (replacement == null) {
                throw new IllegalArgumentException("Undefined environment variable: " + placeholder);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    byte[] resolveBodyPlaceholders(Map<String, String> env) {
        var contentType = headers.get(CONTENT_TYPE_HEADER);
        if (contentType == null || headers.containsKey(CONTENT_ENCODING_HEADER) || !isTextBody(contentType)) {
            return body;
        }
        var charset = getCharset(contentType);
        var bodyText = new String(body, charset);
        if (!PLACEHOLDER_PATTERN.matcher(bodyText).find()) {
            return body;
        }
        return replacePlaceholders(bodyText, env).getBytes(charset);
    }

    private static Charset getCharset(String contentType) {
        var matcher = CHARSET_PATTERN.matcher(contentType);
        if (!matcher.find()) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
    }

    private static boolean isTextBody(String contentType) {
        var separator = contentType.indexOf(';');
        var mediaType = (separator < 0 ? contentType : contentType.substring(0, separator))
                .trim()
                .toLowerCase(Locale.ROOT);
        return mediaType.startsWith("text/") || TEXT_BODY_TYPES.contains(mediaType);
    }

    void saveFieldsToEnvironment(String envFile, Map<String, String> env) {
        var fields = EnvironmentFileLoader.parseEnvFile(Path.of(envFile));
        if (fields.isEmpty()) {
            return;
        }

        final DocumentContext response;
        try {
            response = JsonPath.parse(new String(contentDecoder().apply(body), StandardCharsets.UTF_8));
        } catch (InvalidJsonException e) {
            throw new IllegalStateException("Failed to read the JSON response for " + envFile, e);
        }

        var captured = new HashMap<String, String>();
        fields.forEach((name, expression) -> {
            final Object value;
            try {
                value = response.read(expression);
            } catch (PathNotFoundException e) {
                throw new IllegalStateException("JSONPath '%s' for '%s' is missing in the response"
                        .formatted(expression, name), e);
            } catch (InvalidPathException e) {
                throw new IllegalStateException("JSONPath '%s' for '%s' is invalid"
                        .formatted(expression, name), e);
            }
            if (value instanceof Map<?, ?> || value instanceof Collection<?>) {
                throw new IllegalStateException("JSONPath '%s' for '%s' must select a scalar value"
                        .formatted(expression, name));
            }
            captured.put(name, String.valueOf(value));
        });
        env.putAll(captured);
    }

    /**
     * Resolves Spring-style path variables in the URL path.
     * <p>
     * Replaces path variable placeholders like {@code {variableName}} with values from localEnv.
     * For example, {@code /users/{userId}/orders/{orderId}} with {@code localEnv.get("userId") = "123"}
     * and {@code localEnv.get("orderId") = "456"} becomes {@code /users/123/orders/456}.
     * </p>
     *
     * @param path the URL path containing path variable placeholders
     * @param env  the environment map containing variable values
     * @return the path with resolved variables
     */
    private static String resolvePathVariables(String path, Map<String, String> env) {
        var matcher = PATH_VARIABLE_PATTERN.matcher(path);

        var result = new StringBuilder();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = env.get(variableName);
            if (replacement != null) {
                matcher.appendReplacement(result, replacement);
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Localized text and generated identifiers (descriptions, summaries, operation ids, the application version) are
     * volatile: they change on every wording, ordering or version bump without changing the API shape. The golden
     * keeps them as {@code ***} wildcards to avoid churn, so the body is masked here before it is written.
     */
    private static final Pattern VOLATILE_FIELDS = Pattern
            .compile("(\"(?:description|summary|operationId|version)\"\\s*:\\s*)\".*\"");

    void writeBodyTo(String responseFile) throws IOException {
        try (var rf = new RandomAccessFile(responseFile, "rw")) {
            while (!rf.readLine().isEmpty()) ; // find the first empty string
            rf.setLength(rf.getFilePointer()); // truncate
            var masked = VOLATILE_FIELDS.matcher(new String(body, StandardCharsets.UTF_8)).replaceAll("$1\"***\"")
                    .replace("\r\n", "\n").replace("\n", "\r\n"); // keep the golden in CRLF like the rest of the file
            rf.write(masked.getBytes(StandardCharsets.UTF_8)); // append new body with volatile fields wildcarded
        }
    }

    void assertTo(HttpData expected) throws Exception, AssertionError {
        try {
            assertEquals(expected.getResponseCode(), this.getResponseCode(), "Status code: ");
            for (Map.Entry<String, String> r : expected.headers.entrySet()) {
                String headerName = r.getKey();
                String value = r.getValue();
                Comparators.txt(headerName, value, this.headers.get(headerName));
            }

            if (expected.body == null) {
                return; // No body expected
            }
            var expectedText = new String(expected.body, StandardCharsets.ISO_8859_1).trim();
            if (expectedText.equals("***")) {
                return; // Whole-body wildcard skips content comparison for any content type (json, zip, xml, ...)
            }
            // A body that declares its own framing is read literally, so a reference in it is resolved
            // here - before the content type chooses how to compare, so every type resolves it alike.
            byte[] expectedBody = isFileRef(expectedText)
                    ? readFileRef(expected.pathToResource, expectedText)
                    : expected.body;
            var decoder = contentDecoder();
            String contentType = headers.get(CONTENT_TYPE_HEADER);
            contentType = contentType == null ? "null" : contentType;
            int sep = contentType.indexOf(';');
            if (sep > 0) {
                contentType = contentType.substring(0, sep);
            }
            switch (contentType) {
                case "text/css",
                     "text/javascript",
                     "text/html",
                     "text/plain",
                     "image/svg+xml" ->
                        Comparators.txt("Difference", decoder.apply(expectedBody), decoder.apply(this.body));
                case "application/xml",
                     "text/xml" ->
                        Comparators.xml("Difference", decoder.apply(expectedBody), decoder.apply(this.body));
                case "application/json" -> {
                    JsonNode actualNode = OBJECT_MAPPER.readTree(decoder.apply(this.body));
                    JsonNode expectedNode = OBJECT_MAPPER.readTree(decoder.apply(expectedBody));
                    Comparators.compareJsonObjects(expectedNode, actualNode, "");
                }
                case "application/zip" -> Comparators.zip(decoder.apply(expectedBody), decoder.apply(this.body));
                default -> assertArrayEquals(decoder.apply(expectedBody), decoder.apply(this.body), "Body: ");
            }
        } catch (Exception | AssertionError ex) {
            throw ex;
        }
    }

    private UnaryOperator<byte[]> contentDecoder() {
        var contentEncoding = headers.get(CONTENT_ENCODING_HEADER);
        UnaryOperator<byte[]> decoder = UnaryOperator.identity();
        if (contentEncoding != null) {
            for (String encoding : contentEncoding.split(",", -1)) {
                encoding = encoding.trim().toLowerCase(Locale.ROOT);
                if ("gzip".equals(encoding) || "x-gzip".equals(encoding)) {
                    var previousDecoder = decoder;
                    decoder = bytes -> decodeGzipBytes(previousDecoder.apply(bytes));
                }
            }
        }
        return decoder;
    }

    private static byte[] decodeGzipBytes(byte[] bytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            byte[] buffer = new byte[64 * 1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to decode GZIP input", e); // wrapper
        }
        return out.toByteArray();
    }

    void log(String resourceName) {
        try {
            System.err.println("--------------------");
            System.err.println(firstLine);
            for (Map.Entry<String, String> r : headers.entrySet()) {
                String headerName = r.getKey();
                String value = r.getValue();
                System.err.println(headerName + ": " + value);
            }
            System.err.println();

            System.err.write(body);
            System.err.println("\n--------------------");

            String path = System.getProperty("server.responses") + resourceName + ".body";
            Path responsePath = Path.of(path);
            Files.createDirectories(responsePath.getParent());
            Files.write(responsePath, body);
        } catch (IOException ignored) {
            // Ignored
        }
    }

    private static HttpData readData(HttpResponse<byte[]> connection) {
        String firstLine = "HTTP/1.1 " + connection.statusCode();
        String cookie = null;
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, List<String>> entries : connection.headers().map().entrySet()) {
            if (entries.getKey() != null) {
                if (entries.getKey().equalsIgnoreCase("Set-Cookie")) {
                    cookie = String.join("; ", entries.getValue());
                }
                headers.put(entries.getKey(), String.join(", ", entries.getValue()));
            }
        }

        HttpData httpData = new HttpData(firstLine, headers, connection.body(), null);
        httpData.setCookie(cookie);
        return httpData;
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
        String ct = headers.get(CONTENT_TYPE_HEADER);
        String ce = headers.get(CONTENT_ENCODING_HEADER);

        if (ct != null && ct.startsWith("multipart/form-data") && ct.contains("boundary=")) {
            String boundary = ct.substring(ct.indexOf("boundary=") + "boundary=".length());
            String boundaryEnd = "--" + boundary + "--";
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try (PrintWriter writer = new PrintWriter(os, false, StandardCharsets.UTF_8)) {
                while (true) {
                    String line = readLine(input);
                    if (isFileRef(line)) {
                        writer.flush();
                        os.write(readFileRef(resource, line));
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
        } else if (BLOB_TYPES.contains(ct) || ce != null) {
            String line = readLine(input);
            if (isFileRef(line)) {
                body = readFileRef(resource, line);
                if (input.available() != 0) {
                    throw new IllegalStateException("Unexpected content");
                }
            } else {
                // Inline text: a *** wildcard or a multi-line zip entry spec.
                StringBuilder sb = new StringBuilder(line);
                while (input.available() != 0) {
                    sb.append('\n').append(readLine(input));
                }
                body = sb.toString().getBytes(StandardCharsets.UTF_8);
            }
        } else if (cl != null) {
            body = readBody(input, cl);
        } else if (te != null && te.equalsIgnoreCase("chunked")) {
            body = readChunckedBody(input);
        } else if (NO_CONTENT_STATUS_PATTERN.matcher(firstLine).matches()) {
            // Depending on the implementation of InputStream, reading it can hang if no data is available.
            // So for 204 status we just don't read body because it doesn't needed for this status.
            body = new byte[0];
        } else {
            body = readInlineOrFileRef(input, resource);
        }

        return new HttpData(firstLine, headers, body, resource);
    }

    /**
     * Reads a body of any content type, resolving a body that is nothing but a file reference. It
     * lets a binary payload of a type of its own - a workbook uploaded as {@code application/octet-stream},
     * for one - be kept in a file of its own instead of being inlined into the request.
     */
    private static byte[] readInlineOrFileRef(InputStream input, String resource) throws IOException {
        byte[] content = input.readAllBytes();
        String line = new String(content, StandardCharsets.UTF_8).trim();
        if (!isFileRef(line) || line.indexOf('\n') >= 0) {
            return content;
        }
        return readFileRef(resource, line);
    }

    /**
     * Reads the file a {@code &name} reference names, relative to the folder of the resource the
     * reference was written in.
     */
    private static byte[] readFileRef(String resource, String fileRef) throws IOException {
        String fileRes = resolveFileRef(Path.of(resource).getParent(), fileRef);
        try (InputStream fileStream = getStream(fileRes)) {
            if (fileStream == null) {
                throw new FileNotFoundException(fileRes);
            }
            return fileStream.readAllBytes();
        }
    }

    private static InputStream getStream(String fileRes) {
        try {
            return Files.newInputStream(Path.of(fileRes));
        } catch (IOException e) {
            return HttpData.class.getResourceAsStream(fileRes);
        }
    }

    private static boolean isFileRef(String s) {
        return !s.isEmpty() && s.charAt(0) == '&';
    }

    private static String resolveFileRef(Path parent, String fileRef) {
        return parent.resolve(fileRef.substring(1)).toString().replace('\\', '/');
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
            throw new IOException("Unexpected end of the stream. Expected CRLF in the end of the line.");
        }
        return line.toString();
    }

    public String getCookie() {
        return cookie;
    }

    private void setCookie(String cookie) {
        this.cookie = cookie;
    }
}
