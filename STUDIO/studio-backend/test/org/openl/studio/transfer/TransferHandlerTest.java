package org.openl.studio.transfer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.openl.studio.common.ExceptionMappingService;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.service.files.ConflictPolicy;
import org.openl.studio.session.ClientSessions;

class TransferHandlerTest {

    private final TestClock clock = new TestClock(Instant.parse("2026-10-23T10:00:00Z"));
    private final TransferLinks links = new TransferLinks(clock, "PT15M", 20);
    private final ClientSessions sessions = new ClientSessions(Duration.ofMinutes(30), false);
    private final TransferHandler handler = new TransferHandler(links, sessions, errors(), new ObjectMapper(),
            new InMemoryUserDetailsManager(
                    User.withUsername("jdoe").password("secret").authorities("VIEW_PROJECTS").build(),
                    User.withUsername("locked").password("secret").authorities("VIEW_PROJECTS").accountLocked(true)
                            .build()));

    private String clientKey;

    @BeforeEach
    void keepAClient() {
        // A request of the client with its own credentials, which the client is kept for
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic amRvZTpzZWNyZXQ=");
        var attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
        SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated("jdoe", null, List.of()));
        clientKey = sessions.currentKey();
        attributes.requestCompleted();
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void aDownloadIsForwardedToTheFilesApiAsTheOwnerForAsLongAsTheLinkWorks() throws Exception {
        var id = issue("jdoe", new TransferOperation.Download("P1", "rules/My Rules #1.xlsx", "main", null, false));

        var first = send("GET", id, null);
        var second = send("GET", id, null);

        for (var request : List.of(first, second)) {
            assertEquals(200, request.response.getStatus());
            assertEquals("no-store", request.response.getHeader("Cache-Control"));
            assertEquals("no-referrer", request.response.getHeader("Referrer-Policy"));
            var forward = request.forwards.getFirst();
            assertEquals("/rest/projects/P1/files/rules/My%20Rules%20%231.xlsx", forward.path);
            assertEquals("GET", forward.method);
            assertEquals(Map.of("branch", "main"), forward.parameters);
            assertEquals("jdoe", forward.user.getName());
            assertInstanceOf(TransferLinkAuthentication.class, forward.user);
            assertEquals(clientKey, forward.client);
        }
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void eachPartOfAFilePathIsEncodedWhole() throws Exception {
        var id = issue("jdoe", new TransferOperation.Download("P1", "rules/rates;v2+1%.xlsx", null, null, false));

        var forward = send("GET", id, null).forwards.getFirst();

        assertEquals("/rest/projects/P1/files/rules/rates%3Bv2%2B1%25.xlsx", forward.path);
    }

    @Test
    void anArchiveOfAFolderIsDownloadedFromARevision() throws Exception {
        var id = issue("jdoe", new TransferOperation.Download("P1", "", null, "abc123", true));

        var forward = send("GET", id, null).forwards.getFirst();

        assertEquals("/rest/projects/P1/files/", forward.path);
        assertEquals(Map.of("version", "abc123", "download", "true"), forward.parameters);
    }

    @Test
    void aConflictVersionIsDownloadedFromTheMergeApi() throws Exception {
        var id = issue("jdoe", new TransferOperation.ConflictVersion("P1", "rules/Rules.xlsx", ConflictBase.THEIRS));

        var forward = send("GET", id, null).forwards.getFirst();

        assertEquals("/rest/projects/P1/merge/conflicts/files", forward.path);
        assertEquals(Map.of("file", "rules/Rules.xlsx", "side", "theirs"), forward.parameters);
    }

    @Test
    void anUploadWritesItsBodyOnceThroughTheFilesApi() throws Exception {
        var body = "<project/>".getBytes(StandardCharsets.UTF_8);
        var id = issue("jdoe", upload().branch("main").conflictPolicy(ConflictPolicy.OVERWRITE)
                .size((long) body.length).sha256(sha256(body)).build());

        var first = send("PUT", id, body);
        var second = send("PUT", id, body);

        var forward = first.forwards.getFirst();
        assertEquals("/rest/projects/P1/files/rules.xml", forward.path);
        assertEquals("POST", forward.method);
        assertEquals("application/octet-stream", forward.contentType);
        assertEquals(Map.of("createFolders", "false", "conflictPolicy", "OVERWRITE", "branch", "main"),
                forward.parameters);
        assertArrayEquals(body, forward.body);
        assertNull(forward.ifMatch);
        assertEquals(404, second.response.getStatus());
        assertTrue(second.forwards.isEmpty());
    }

    @Test
    void anUploadOfAKnownVersionReplacesTheFileWhileItHasThatVersion() throws Exception {
        var body = "<project/>".getBytes(StandardCharsets.UTF_8);
        var id = issue("jdoe", upload().branch("main").createFolders(true).expectedVersion("v1").build());

        var forward = send("PUT", id, body, "\"v0\"").forwards.getFirst();

        assertEquals("/rest/projects/P1/files/rules.xml", forward.path);
        assertEquals("PUT", forward.method);
        assertEquals("application/octet-stream", forward.contentType);
        assertEquals(Map.of("branch", "main"), forward.parameters);
        // The link names the version; the one the client sent beside it is not used.
        assertEquals("\"v1\"", forward.ifMatch);
        assertArrayEquals(body, forward.body);
    }

    @Test
    void anUploadNotMatchingWhatWasDeclaredWritesNothing() throws Exception {
        var id = issue("jdoe", upload().sha256(sha256("<project/>".getBytes(StandardCharsets.UTF_8))).build());

        var sent = send("PUT", id, "<project>cut".getBytes(StandardCharsets.UTF_8));

        assertEquals(400, sent.response.getStatus());
        assertTrue(sent.response.getContentAsString().contains("transfer.upload.mismatch.message"));
        assertTrue(sent.forwards.isEmpty());
        assertNull(links.find(id));
    }

    @Test
    void aLinkUsedWithTheWrongMethodIsRefusedAndKept() throws Exception {
        var download = issue("jdoe", new TransferOperation.Download("P1", "rules.xml", null, null, false));
        var upload = issue("jdoe", upload().build());

        assertEquals(404, send("PUT", download, new byte[0]).response.getStatus());
        assertEquals(404, send("GET", upload, null).response.getStatus());
        assertEquals(404, send("DELETE", download, null).response.getStatus());

        assertEquals(1, send("GET", download, null).forwards.size());
        assertEquals(1, send("PUT", upload, new byte[0]).forwards.size());
    }

    @Test
    void anUnknownOrExpiredLinkIsRefusedAlike() throws Exception {
        var expired = issue("jdoe", new TransferOperation.Download("P1", "rules.xml", null, null, false));
        clock.advance(Duration.ofMinutes(15));

        for (var sent : List.of(send("GET", expired, null), send("GET", "unknown", null),
                send("GET", "a/b", null), send("GET", "", null))) {
            assertEquals(404, sent.response.getStatus());
            assertEquals("application/json", sent.response.getContentType());
            assertTrue(sent.response.getContentAsString().contains("""
                    "code":"openl.error.404.transfer.link\
                    .unknown.message"\
                    """));
            assertTrue(sent.forwards.isEmpty());
        }
    }

    @Test
    void aLinkOfAUserWhoCannotActAnyMoreIsRefused() throws Exception {
        var locked = issue("locked", new TransferOperation.Download("P1", "rules.xml", null, null, false));
        var removed = issue("removed", new TransferOperation.Download("P1", "rules.xml", null, null, false));

        assertEquals(404, send("GET", locked, null).response.getStatus());
        assertEquals(404, send("GET", removed, null).response.getStatus());
    }

    @Test
    void aLinkOfAClientWhoseStateEndedIsRefused() throws Exception {
        var issued = links.add("jdoe", "jdoe|pat:gone", new TransferOperation.Download("P1", "rules.xml", null,
                null, false));

        var sent = send("GET", issued.id(), null);

        assertEquals(404, sent.response.getStatus());
        assertTrue(sent.forwards.isEmpty());
    }

    private String issue(String user, TransferOperation operation) {
        return links.add(user, clientKey, operation).id();
    }

    private Sent send(String method, String id, byte[] body) throws Exception {
        return send(method, id, body, null);
    }

    private Sent send(String method, String id, byte[] body, String ifMatch) throws Exception {
        var request = new LinkRequest(method, id);
        if (body != null) {
            request.setContent(body);
        }
        if (ifMatch != null) {
            request.addHeader(HttpHeaders.IF_MATCH, ifMatch);
        }
        var response = new MockHttpServletResponse();
        handler.handle(request, response);
        return new Sent(response, request.forwards);
    }

    /** An upload of rules.xml of P1 that fails on a file already there, the other fields left to each test. */
    private static TransferOperation.Upload.UploadBuilder upload() {
        return TransferOperation.Upload.builder().projectId("P1").path("rules.xml").conflictPolicy(ConflictPolicy.FAIL);
    }

    private static String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }

    private static ExceptionMappingService errors() {
        var messages = new ReloadableResourceBundleMessageSource();
        messages.setBasename("classpath:ValidationMessages");
        messages.setDefaultEncoding("UTF-8");
        return new ExceptionMappingService(messages);
    }

    private record Sent(MockHttpServletResponse response, List<Forward> forwards) {
    }

    private record Forward(String path, String method, Map<String, String> parameters, String contentType,
                           String ifMatch, byte[] body, Authentication user, String client) {
    }

    /** A request to a link that records where it is forwarded, and what the endpoint there would see. */
    private final class LinkRequest extends MockHttpServletRequest {

        private final List<Forward> forwards = new ArrayList<>();

        LinkRequest(String method, String id) {
            super(method, "/transfers/" + id);
            setServletPath("/transfers");
            setPathInfo(id.isEmpty() ? null : "/" + id);
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path) {
            return new RequestDispatcher() {
                @Override
                public void forward(ServletRequest request, ServletResponse response) throws IOException {
                    var forwarded = (HttpServletRequest) request;
                    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(forwarded));
                    var parameters = new TreeMap<String, String>();
                    forwarded.getParameterMap().forEach((name, values) -> parameters.put(name, values[0]));
                    forwards.add(new Forward(path, forwarded.getMethod(), Map.copyOf(parameters),
                            forwarded.getContentType(), forwarded.getHeader(HttpHeaders.IF_MATCH),
                            forwarded.getInputStream().readAllBytes(),
                            SecurityContextHolder.getContext().getAuthentication(), sessions.currentKey()));
                    RequestContextHolder.resetRequestAttributes();
                }

                @Override
                public void include(ServletRequest request, ServletResponse response) {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
