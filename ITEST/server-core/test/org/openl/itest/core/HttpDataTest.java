package org.openl.itest.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HttpDataTest {

    @Test
    void testReadFile() throws Exception {
        HttpData chuncked = HttpData.readFile("/chuncked.resp");
        HttpData clHeader = HttpData.readFile("/content-length.resp");
        HttpData undefined = HttpData.readFile("/undefined-length.resp");

        chuncked.assertTo(undefined);
        clHeader.assertTo(undefined);
        undefined.assertTo(undefined);

        assertEquals("Header Content-Length ==> missing, expected: <14>", failure(undefined, clHeader));
        assertEquals("Header Transfer-Encoding ==> missing, expected: <chunked>", failure(undefined, chuncked));
    }

    @Test
    void statusCodeMismatchIsNamed() throws IOException {
        var actual = HttpData.readFile("/undefined-length.resp");
        var expected = HttpData.readFile("/no-content-full.resp");

        assertEquals("Status code ==> expected: <204> but was: <200>", failure(actual, expected));
    }

    @Test
    void textBodyMismatchShowsBothTexts(@TempDir Path tempDir) throws IOException {
        var actual = HttpData.readFile("/undefined-length.resp");
        var expected = createResponse(tempDir.resolve("text.resp"), "text/plain", "Hello John!");

        assertEquals("Body ==> expected: <Hello John!> but was: <Hello World!>", failure(actual, expected));
    }

    @Test
    void csvBodyIsComparedAsTextWithWildcards(@TempDir Path tempDir) throws IOException {
        var csv = "text/csv;charset=UTF-8";
        var actual = createResponse(tempDir.resolve("actual.resp"), csv, "time,actor\r\n2026-10-19T10:15:30Z,jdoe");
        var matching = createResponse(tempDir.resolve("matching.resp"), csv, "time,actor\r\n***,jdoe");
        var different = createResponse(tempDir.resolve("different.resp"), csv, "time,actor\r\n***,admin");

        assertDoesNotThrow(() -> actual.assertTo(matching));
        assertTrue(failure(actual, different).startsWith("Body ==> expected: <time,actor"));
    }

    @Test
    void jsonBodyMismatchShowsThePathToIt(@TempDir Path tempDir) throws IOException {
        var actual = createResponse(tempDir.resolve("actual.resp"), "application/json", "{\"name\": \"foo\"}");
        var expected = createResponse(tempDir.resolve("expected.resp"), "application/json", "{\"name\": \"bar\"}");

        assertEquals("Body > name ==> expected: <\"bar\"> but was: <\"foo\">", failure(actual, expected));
    }

    @Test
    void binaryBodyMismatchIsNamed(@TempDir Path tempDir) throws IOException {
        var actual = createResponse(tempDir.resolve("actual.resp"), "image/png", "12345");
        var expected = createResponse(tempDir.resolve("expected.resp"), "image/png", "1234");

        assertEquals("Body ==> array lengths differ, expected: <6> but was: <7>", failure(actual, expected));
    }

    /** An event stream is compared as text, so its events match the wildcards of the expected one. */
    @Test
    void eventStreamBodyIsComparedAsText(@TempDir Path tempDir) throws IOException {
        var actual = createResponse(tempDir.resolve("actual.resp"), "text/event-stream",
                "event: message\ndata: {\"result\":{\"version\":\"1.2.3\"},\"id\":1}\n");
        var expected = createResponse(tempDir.resolve("expected.resp"), "text/event-stream",
                "event: message\r\ndata: {\"result\":{\"version\":\"*\"},\"id\":1}");
        var otherId = createResponse(tempDir.resolve("other.resp"), "text/event-stream",
                "event: message\r\ndata: {\"result\":{\"version\":\"*\"},\"id\":2}");

        actual.assertTo(expected);
        assertThrows(AssertionError.class, () -> actual.assertTo(otherId));
    }

    private static @Nullable String failure(HttpData actual, HttpData expected) {
        return assertThrows(AssertionError.class, () -> actual.assertTo(expected)).getMessage();
    }

    @Test
    void testReadAbsentFile() throws IOException {
        HttpData absent = HttpData.readFile("/absent");
        assertNull(absent);
    }

    @Test
    void testWrongLength() {
        var er = assertThrows(IOException.class, () -> HttpData.readFile("/wrong-length.resp"));
        assertEquals("Unexpected size of the body.", er.getMessage());
    }

    @Test
    void testWrongHeader() {
        var er = assertThrows(IOException.class, () -> HttpData.readFile("/wrong-header.resp"));
        assertEquals("Unexpected end of the stream. Expected CRLF in the end of the line.", er.getMessage());
    }

    @Test
    void testWrongChuncked() {
        var er = assertThrows(IOException.class, () -> HttpData.readFile("/wrong-chuncked.resp"));
        assertEquals("Unexpected format of the chunk.", er.getMessage());

        er = assertThrows(IOException.class, () -> HttpData.readFile("/wrong-chuncked2.resp"));
        assertEquals("Unexpected size of the chunk.", er.getMessage());
    }

    /**
     * A body that is nothing but a file reference is read from that file, whatever the content type
     * is. It lets a binary payload - a workbook uploaded as {@code application/octet-stream} - be
     * kept in a file of its own instead of being inlined into the request.
     */
    @Test
    void testFileRefBody() throws Exception {
        HttpData fileRef = HttpData.readFile("/file-ref-body.resp");
        HttpData inline = HttpData.readFile("/undefined-length.resp");
        fileRef.assertTo(inline);
        inline.assertTo(fileRef);
    }

    @Test
    void testResponse204() throws Exception {
        final HttpData fullNoContent = HttpData.readFile("/no-content-full.resp");
        final HttpData shortNoContent = HttpData.readFile("/no-content-short.resp");
        final HttpData noContentWithBody = HttpData.readFile("/no-content-non-standard.resp");
        fullNoContent.assertTo(shortNoContent);
        fullNoContent.assertTo(noContentWithBody);
        shortNoContent.assertTo(noContentWithBody);
    }

    @Test
    void responseFieldsAreSavedToEnvironment() throws IOException {
        var response = HttpData.readFile("/response-fields.resp");
        var environment = new HashMap<String, String>();

        response.saveFieldsToEnvironment("test-resources/response-fields.env", environment);

        assertEquals(Map.of("REVISION", "abc123", "COUNT", "2", "READY", "true"), environment);
    }

    @Test
    void responseFieldsAreSavedFromGzipResponse(@TempDir Path tempDir) throws IOException {
        var compressedBody = tempDir.resolve("response-fields.json.gz");
        try (var gzip = new GZIPOutputStream(Files.newOutputStream(compressedBody))) {
            gzip.write("{\"revision\":\"abc123\"}".getBytes(StandardCharsets.UTF_8));
        }
        var responseFile = tempDir.resolve("response-fields.resp");
        Files.writeString(responseFile,
                String.join("\r\n",
                        "HTTP/1.1 200",
                        "Content-Type: application/json",
                        "Content-Encoding: GZip",
                        "",
                        "&response-fields.json.gz",
                        ""),
                StandardCharsets.UTF_8);
        var environmentFile = tempDir.resolve("response-fields.env");
        Files.writeString(environmentFile, "REVISION=$.revision", StandardCharsets.UTF_8);
        var response = HttpData.readFile(responseFile.toString());
        var environment = new HashMap<String, String>();

        response.saveFieldsToEnvironment(environmentFile.toString(), environment);

        assertEquals(Map.of("REVISION", "abc123"), environment);
    }

    @Test
    void responseFieldsAreSavedFromNestedGzipResponse(@TempDir Path tempDir) throws IOException {
        var compressedBody = tempDir.resolve("response-fields.json.gz.gz");
        var json = "{\"revision\":\"abc123\"}".getBytes(StandardCharsets.UTF_8);
        Files.write(compressedBody, gzip(gzip(json)));
        var responseFile = tempDir.resolve("response-fields.resp");
        Files.writeString(responseFile,
                String.join("\r\n",
                        "HTTP/1.1 200",
                        "Content-Type: application/json",
                        "Content-Encoding: gzip, gzip",
                        "",
                        "&response-fields.json.gz.gz",
                        ""),
                StandardCharsets.UTF_8);
        var environmentFile = tempDir.resolve("response-fields.env");
        Files.writeString(environmentFile, "REVISION=$.revision", StandardCharsets.UTF_8);
        var response = HttpData.readFile(responseFile.toString());
        var environment = new HashMap<String, String>();

        response.saveFieldsToEnvironment(environmentFile.toString(), environment);

        assertEquals(Map.of("REVISION", "abc123"), environment);
    }

    @Test
    void missingResponseFieldIsRejected() throws IOException {
        var response = HttpData.readFile("/response-fields.resp");

        var error = assertThrows(IllegalStateException.class,
                () -> response.saveFieldsToEnvironment(
                        "test-resources/missing-response-field.env",
                        new HashMap<>()));

        assertEquals("JSONPath '$.content[2].revisionNo' for 'REVISION' is missing in the response",
                error.getMessage());
    }

    @Test
    void structuredResponseFieldIsRejectedWithoutChangingEnvironment() throws IOException {
        var response = HttpData.readFile("/response-fields.resp");
        var environment = new HashMap<>(Map.of("EXISTING", "value"));

        var error = assertThrows(IllegalStateException.class,
                () -> response.saveFieldsToEnvironment(
                        "test-resources/structured-response-field.env",
                        environment));

        assertEquals("JSONPath '$.content' for 'CONTENT' must select a scalar value", error.getMessage());
        assertEquals(Map.of("EXISTING", "value"), environment);
    }

    @Test
    void absentResponseEnvironmentFileDoesNothing() throws IOException {
        var response = HttpData.readFile("/response-fields.resp");
        var environment = new HashMap<String, String>();

        response.saveFieldsToEnvironment("test-resources/absent.env", environment);

        assertEquals(Map.of(), environment);
    }

    /** A header is captured whatever the body is: an event stream is not read as JSON. */
    @Test
    void responseHeaderIsSavedToEnvironment(@TempDir Path tempDir) throws IOException {
        var responseFile = tempDir.resolve("session.resp");
        Files.writeString(responseFile,
                String.join("\r\n",
                        "HTTP/1.1 200",
                        "Content-Type: text/event-stream",
                        "Mcp-Session-Id: abc-123",
                        "",
                        "event: message",
                        "data: {}",
                        ""),
                StandardCharsets.UTF_8);
        var environmentFile = tempDir.resolve("session.env");
        Files.writeString(environmentFile, "SESSION=header:mcp-session-id", StandardCharsets.UTF_8);
        var environment = new HashMap<String, String>();

        HttpData.readFile(responseFile.toString()).saveFieldsToEnvironment(environmentFile.toString(), environment);

        assertEquals(Map.of("SESSION", "abc-123"), environment);
    }

    @Test
    void missingResponseHeaderIsRejected(@TempDir Path tempDir) throws IOException {
        var response = createResponse(tempDir.resolve("session.resp"), "text/event-stream", "event: message");
        var environmentFile = tempDir.resolve("session.env");
        Files.writeString(environmentFile, "SESSION=header:Mcp-Session-Id", StandardCharsets.UTF_8);

        var environment = environmentFile.toString();
        var values = new HashMap<String, String>();

        var error = assertThrows(IllegalStateException.class, () -> response.saveFieldsToEnvironment(environment, values));

        assertEquals("Header 'Mcp-Session-Id' for 'SESSION' is missing in the response", error.getMessage());
    }

    @Test
    void placeholdersAreResolvedInJsonRequestBody() throws IOException {
        var request = HttpData.readFile("/request-body-placeholder.req");

        var body = request.resolveBodyPlaceholders(Map.of("REVISION", "abc123"));

        assertEquals("{\r\n  \"revision\": \"abc123\"\r\n}\r\n", new String(body, StandardCharsets.UTF_8));
    }

    @Test
    void placeholdersUseDeclaredRequestCharset(@TempDir Path tempDir) throws IOException {
        var request = createRequest(
                tempDir.resolve("iso-8859-1.req"),
                "text/plain; charset=ISO-8859-1",
                "café ${NAME}".getBytes(StandardCharsets.ISO_8859_1));

        var body = request.resolveBodyPlaceholders(Map.of("NAME", "élève"));

        assertArrayEquals("café élève".getBytes(StandardCharsets.ISO_8859_1), body);
    }

    @Test
    void placeholderFreeRequestBodyIsNotReencoded(@TempDir Path tempDir) throws IOException {
        var originalBody = "café".getBytes(StandardCharsets.ISO_8859_1);
        var request = createRequest(tempDir.resolve("placeholder-free.req"), "text/plain", originalBody);

        var body = request.resolveBodyPlaceholders(Map.of());

        assertArrayEquals(originalBody, body);
    }

    @Test
    void encodedRequestBodyIsNotModified(@TempDir Path tempDir) throws IOException {
        var compressedBody = tempDir.resolve("request.json.gz");
        try (var gzip = new GZIPOutputStream(Files.newOutputStream(compressedBody))) {
            gzip.write("{\"revision\":\"${REVISION}\"}".getBytes(StandardCharsets.UTF_8));
        }
        var requestFile = tempDir.resolve("request.req");
        Files.writeString(requestFile,
                String.join("\r\n",
                        "POST / HTTP/1.1",
                        "Content-Type: text/plain;q=1.0, */*;q=0.8",
                        "Content-Encoding: gzip",
                        "",
                        "&request.json.gz",
                        ""),
                StandardCharsets.UTF_8);
        var request = HttpData.readFile(requestFile.toString());

        var body = request.resolveBodyPlaceholders(Map.of("REVISION", "abc123"));

        assertArrayEquals(Files.readAllBytes(compressedBody), body);
    }

    private static HttpData createResponse(Path responseFile, String contentType, String body) throws IOException {
        Files.writeString(responseFile,
                "HTTP/1.1 200\r\nContent-Type: " + contentType + "\r\n\r\n" + body + "\r\n",
                StandardCharsets.UTF_8);
        return HttpData.readFile(responseFile.toString());
    }

    // HTTP ends header lines with CRLF whatever the platform.
    @SuppressWarnings("java:S3457")
    private static HttpData createRequest(Path requestFile, String contentType, byte[] body) throws IOException {
        Files.writeString(requestFile,
                "POST / HTTP/1.1\r\nContent-Type: %s\r\n\r\n".formatted(contentType),
                StandardCharsets.US_ASCII);
        Files.write(requestFile, body, StandardOpenOption.APPEND);
        return HttpData.readFile(requestFile.toString());
    }

    private static byte[] gzip(byte[] body) throws IOException {
        var output = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(output)) {
            gzip.write(body);
        }
        return output.toByteArray();
    }
}
