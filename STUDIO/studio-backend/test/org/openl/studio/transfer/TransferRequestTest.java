package org.openl.studio.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

/** A request to a transfer link is read as the link recorded it, whatever the client sent besides. */
class TransferRequestTest {

    @Test
    void anUploadOfAKnownVersionNamesThatVersionAndNotTheOneTheClientSent() throws IOException {
        var client = clientRequest();
        var body = new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8));

        var request = TransferRequest.put(client, Map.of("branch", "main"), body, "v2");

        assertEquals("PUT", request.getMethod());
        assertEquals("\"v2\"", request.getHeader("if-match"));
        assertEquals(List.of("\"v2\""), Collections.list(request.getHeaders(HttpHeaders.IF_MATCH)));
        assertEquals(List.of("X-Client", HttpHeaders.IF_MATCH), Collections.list(request.getHeaderNames()));
        assertEquals("agent", request.getHeader("X-Client"));
        assertEquals(List.of("agent"), Collections.list(request.getHeaders("X-Client")));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_VALUE, request.getContentType());
        assertEquals("content", new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void aRequestOfNoVersionNamesNoneWhateverTheClientSent() throws IOException {
        var body = new ByteArrayInputStream(new byte[0]);

        for (var request : List.of(TransferRequest.get(clientRequest(), Map.of()),
                TransferRequest.post(clientRequest(), Map.of(), body))) {
            assertNull(request.getHeader(HttpHeaders.IF_MATCH));
            assertEquals(List.of(), Collections.list(request.getHeaders(HttpHeaders.IF_MATCH)));
            assertEquals(List.of("X-Client"), Collections.list(request.getHeaderNames()));
        }
        var download = TransferRequest.get(clientRequest(), Map.of());
        assertNull(download.getContentType());
        assertEquals(0, download.getInputStream().readAllBytes().length, "a download has no body");
    }

    /** What a client sends to the link: a version of its own and a header of its own. */
    private static MockHttpServletRequest clientRequest() {
        var client = new MockHttpServletRequest("PUT", "/transfers/link");
        client.addHeader(HttpHeaders.IF_MATCH, "\"v1\"");
        client.addHeader("X-Client", "agent");
        return client;
    }
}
