package org.openl.itest.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

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

        try {
            undefined.assertTo(clHeader);
            fail("Non reachable");
        } catch (AssertionError er) {
            assertEquals("Content-Length ==> expected: <14> but was: <null>", er.getMessage());
        }

        try {
            undefined.assertTo(chuncked);
            fail("Non reachable");
        } catch (AssertionError er) {
            assertEquals("Transfer-Encoding ==> expected: <chunked> but was: <null>", er.getMessage());
        }
    }

    @Test
    void testReadAbsentFile() throws IOException {
        HttpData absent = HttpData.readFile("/absent");
        assertNull(absent);
    }

    @Test
    void testWrongLength() {
        try {
            HttpData.readFile("/wrong-length.resp");
            fail("Non reachable");
        } catch (IOException er) {
            assertEquals("Unexpected size of the body.", er.getMessage());
        }
    }

    @Test
    void testWrongHeader() {
        try {
            HttpData.readFile("/wrong-header.resp");
            fail("Non reachable");
        } catch (IOException er) {
            assertEquals("Unexpected end of the stream. Expected CRLF in the end of the line.", er.getMessage());
        }
    }

    @Test
    void testWrongChuncked() {
        try {
            HttpData.readFile("/wrong-chuncked.resp");
            fail("Non reachable");
        } catch (IOException er) {
            assertEquals("Unexpected format of the chunk.", er.getMessage());
        }

        try {
            HttpData.readFile("/wrong-chuncked2.resp");
            fail("Non reachable");
        } catch (IOException er) {
            assertEquals("Unexpected size of the chunk.", er.getMessage());
        }
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
