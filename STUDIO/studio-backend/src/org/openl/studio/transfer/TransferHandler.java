package org.openl.studio.transfer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import org.openl.rules.webstudio.web.servlet.StudioDispatcherServlet;
import org.openl.studio.common.ExceptionMappingService;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.exception.RestRuntimeException;
import org.openl.studio.common.validation.FileIntegrityValidator;
import org.openl.studio.session.ClientSessions;
import org.openl.util.FileUtils;

/**
 * Runs the operation of a transfer link a request is sent to.
 *
 * <p>The link is the only credential the request needs: it is looked up before a byte of the body is read, so a
 * request without a valid link is answered at once, and an unknown, an expired, a spent link and one used with the
 * wrong method are refused alike. The request then acts as the user who asked for the link, with the authorities
 * the user has now, and for the client the link was issued to, so it finds the state that client keeps.
 *
 * <p>The operation is run by the REST endpoint that runs it for any other client: the request is forwarded there
 * with the method, the parameters and the body the link names. The endpoint checks the rights of the user and
 * answers as it always does. An upload is received into a temporary file first, so its declared size and SHA-256
 * are checked before anything is written.
 *
 * @author Yury Molchan
 */
@Slf4j
@Component
@ConditionalOnExpression("'${user.mode}' != 'single'")
@RequiredArgsConstructor
public class TransferHandler {

    private static final AccountStatusUserDetailsChecker ACCOUNT_STATUS = new AccountStatusUserDetailsChecker();
    private static final String PROJECTS = StudioDispatcherServlet.REST_PATH + "/projects/";

    private final TransferLinks links;
    private final ClientSessions clientSessions;
    private final ExceptionMappingService errors;
    private final ObjectMapper json;
    @Qualifier("patUserInfoUserDetailsService")
    private final UserDetailsService users;

    /**
     * Answers a request to {@code /transfers/<id>}.
     */
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        var id = idOf(request.getPathInfo());
        var link = id == null ? null : linkFor(request.getMethod(), id);
        var owner = link == null ? null : ownerOf(link.user());
        var leave = owner == null ? null : clientSessions.enter(request, link.clientKey());
        if (leave == null) {
            refuse(response, new NotFoundException("transfer.link.unknown.message"));
            return;
        }
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(owner);
        SecurityContextHolder.setContext(context);
        try {
            run(link.operation(), request, response);
        } finally {
            SecurityContextHolder.clearContext();
            leave.run();
        }
    }

    /** The id of a link from the path below {@code /transfers}, or null when the path names none. */
    private static @Nullable String idOf(@Nullable String pathInfo) {
        if (pathInfo == null || pathInfo.length() < 2 || pathInfo.indexOf('/', 1) >= 0) {
            return null;
        }
        return pathInfo.substring(1);
    }

    /**
     * The link a request of the given method may use: a download answers {@code GET} for as long as it works, and an
     * upload answers one {@code PUT}, taken away so that no other request finds it.
     */
    private @Nullable TransferLink linkFor(String method, String id) {
        return switch (method) {
            case "GET" -> {
                var link = links.find(id);
                yield link == null || link.operation().once() ? null : link;
            }
            case "PUT" -> links.takeUpload(id);
            default -> null;
        };
    }

    /** The user the link runs as, with the authorities the user has now, or null when the user cannot act. */
    private @Nullable TransferLinkAuthentication ownerOf(String user) {
        try {
            var details = users.loadUserByUsername(user);
            ACCOUNT_STATUS.check(details);
            return new TransferLinkAuthentication(details, details.getAuthorities());
        } catch (UsernameNotFoundException | AccountStatusException e) {
            return null;
        }
    }

    private void run(TransferOperation operation, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        switch (operation) {
            case TransferOperation.Download download -> forward(request, response, filesPath(download.projectId(),
                    download.path()), TransferRequest.get(request, downloadParameters(download)));
            case TransferOperation.ConflictVersion(var projectId, var file, var side) -> forward(request, response,
                    PROJECTS + projectId + "/merge/conflicts/files",
                    TransferRequest.get(request, Map.of("file", file, "side", side.name().toLowerCase(Locale.ROOT))));
            case TransferOperation.Upload upload -> upload(upload, request, response);
        }
    }

    private void upload(TransferOperation.Upload upload, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        var digest = DigestUtils.getSha256Digest();
        // The file is created before the body is read, so its path never depends on what the request sends
        var body = FileUtils.createPrivateTempFile("openl-transfer", ".tmp");
        try {
            long size;
            try {
                size = FileUtils.copyBounded(new DigestInputStream(request.getInputStream(), digest), body,
                        FileIntegrityValidator.MAX_BUFFERED_BYTES);
            } catch (FileUtils.ContentTooLargeException e) {
                refuse(response, new BadRequestException("file.content.too-large.message",
                        new Object[]{upload.path(), e.getMaxBytes()}));
                return;
            }
            var sha256 = HexFormat.of().formatHex(digest.digest());
            if ((upload.size() != null && upload.size() != size)
                    || (upload.sha256() != null && !upload.sha256().equalsIgnoreCase(sha256))) {
                refuse(response, new BadRequestException("transfer.upload.mismatch.message"));
                return;
            }
            var parameters = new LinkedHashMap<String, String>();
            parameters.put("createFolders", Boolean.toString(upload.createFolders()));
            parameters.put("conflictPolicy", upload.conflictPolicy().name());
            if (upload.branch() != null) {
                parameters.put("branch", upload.branch());
            }
            try (var in = Files.newInputStream(body)) {
                forward(request, response, filesPath(upload.projectId(), upload.path()),
                        TransferRequest.post(request, parameters, in));
            }
        } finally {
            Files.deleteIfExists(body);
        }
    }

    private static Map<String, String> downloadParameters(TransferOperation.Download download) {
        var parameters = new LinkedHashMap<String, String>();
        if (download.branch() != null) {
            parameters.put("branch", download.branch());
        }
        if (download.version() != null) {
            parameters.put("version", download.version());
        }
        if (download.archive()) {
            parameters.put("download", "true");
        }
        return parameters;
    }

    /**
     * The address of a file of a project in the files API, each part of its path encoded whole: a {@code ;} of a file
     * name would otherwise start the parameters of a path segment, which Spring leaves out of the path it matches.
     */
    private static String filesPath(String projectId, String path) {
        return PROJECTS + projectId + "/files/" + Arrays.stream(path.split("/", -1))
                .map(part -> UriUtils.encode(part, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
    }

    private static void forward(HttpServletRequest request, HttpServletResponse response, String path,
                                TransferRequest transfer) throws IOException, ServletException {
        request.getRequestDispatcher(path).forward(transfer, response);
    }

    /** Answers a refusal in the form every REST endpoint answers one. */
    private void refuse(HttpServletResponse response, RestRuntimeException refusal) throws IOException {
        log.debug("A transfer is refused: {}", refusal.getMessage());
        response.setStatus(Objects.requireNonNullElse(refusal.getHttpStatus(), HttpStatus.INTERNAL_SERVER_ERROR)
                .value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getOutputStream(), errors.processException(refusal));
    }

}
