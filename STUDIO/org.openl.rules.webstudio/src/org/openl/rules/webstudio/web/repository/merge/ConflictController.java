package org.openl.rules.webstudio.web.repository.merge;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.util.WebTool;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.IOUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/conflict")
@Tag(name = "Conflict")
public class ConflictController {
    private static final Logger LOG = LoggerFactory.getLogger(ConflictController.class);

    private final MultiUserWorkspaceManager workspaceManager;
    private final UserManagementService userManagementService;

    @Autowired
    public ConflictController(MultiUserWorkspaceManager workspaceManager, UserManagementService userManagementService) {
        this.workspaceManager = workspaceManager;
        this.userManagementService = userManagementService;
    }

    @GetMapping(value = "/repository", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "conflict.repository.summary", description = "conflict.repository.desc")
    @ApiResponse(responseCode = "200", description = "conflict.repository.200.desc", headers = {
            @Header(name = HttpHeaders.CONTENT_DISPOSITION, description = "conflict.header.content-disposition.desc", required = true),
            @Header(name = HttpHeaders.SET_COOKIE, description = "conflict.header.set-cookie.desc") }, content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<?> repository(
            @Parameter(description = "conflict.field.repo-id") @RequestParam(Constants.REQUEST_PARAM_REPO_ID) final String repoId,
            @Parameter(description = "conflict.field.name") @RequestParam(Constants.REQUEST_PARAM_NAME) final String name,
            @Parameter(description = "conflict.field.version") @RequestParam(Constants.REQUEST_PARAM_VERSION) final String version,
            @RequestParam(value = Constants.RESPONSE_MONITOR_COOKIE, required = false) String cookieId,
            HttpServletRequest request,
            HttpServletResponse response) {

        String cookieName = Constants.RESPONSE_MONITOR_COOKIE + "_" + cookieId;
        StreamingResponseBody streamingOutput = output -> {
            FileItem file = workspaceManager.getUserWorkspace(getUser())
                .getDesignTimeRepository()
                .getRepository(repoId)
                .readHistory(name, version);
            if (file == null) {
                throw new FileNotFoundException(String.format("File '%s' is not found.", name));
            }
            try (var stream = file.getStream()) {
                IOUtils.copy(stream, output);
            } finally {
                output.flush();
            }
        };

        return prepareResponse(request, response, cookieName, name, streamingOutput);
    }

    @GetMapping(value = "/local", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "conflict.local.summary", description = "conflict.local.desc")
    @ApiResponse(responseCode = "200", description = "conflict.local.200.desc", headers = {
            @Header(name = HttpHeaders.CONTENT_DISPOSITION, description = "conflict.header.content-disposition.desc", required = true),
            @Header(name = HttpHeaders.SET_COOKIE, description = "conflict.header.set-cookie.desc") }, content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<?> local(
            @Parameter(description = "conflict.field.repo-id") @RequestParam(Constants.REQUEST_PARAM_REPO_ID) final String repoId,
            @Parameter(description = "conflict.field.name") @RequestParam(Constants.REQUEST_PARAM_NAME) final String name,
            @RequestParam(value = Constants.RESPONSE_MONITOR_COOKIE, required = false) String cookieId,
            HttpServletRequest request,
            HttpServletResponse response) {

        String cookieName = Constants.RESPONSE_MONITOR_COOKIE + "_" + cookieId;
        StreamingResponseBody streamingOutput = output -> {
            try {
                UserWorkspace userWorkspace = workspaceManager.getUserWorkspace(getUser());
                Optional<RulesProject> projectByPath = userWorkspace.getProjectByPath(repoId, name);
                if (projectByPath.isPresent()) {
                    RulesProject project = projectByPath.get();
                    String artefactPath = name.substring(project.getRealPath().length() + 1);
                    if (project.hasArtefact(artefactPath)) {
                        try (var stream = ((AProjectResource) project.getArtefact(artefactPath)).getContent()) {
                            IOUtils.copy(stream, output);
                            return;
                        } finally {
                            output.flush();
                        }
                    }
                }
                throw new FileNotFoundException(String.format("File %s is not found.", name));
            } catch (ProjectException e) {
                LOG.warn(e.getMessage(), e);
                throw new IOException(e.getMessage(), e);
            }
        };

        return prepareResponse(request, response, cookieName, name, streamingOutput);
    }

    @GetMapping(value = "/merged", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "conflict.merged.summary", description = "conflict.merged.desc")
    @ApiResponse(responseCode = "200", description = "conflict.merged.200.desc", headers = {
            @Header(name = HttpHeaders.CONTENT_DISPOSITION, description = "conflict.header.content-disposition.desc", required = true),
            @Header(name = HttpHeaders.SET_COOKIE, description = "conflict.header.set-cookie.desc") }, content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<?> merged(
            @Parameter(description = "conflict.field.name") @RequestParam(Constants.REQUEST_PARAM_NAME) final String name,
            @RequestParam(value = Constants.RESPONSE_MONITOR_COOKIE, required = false) String cookieId,
            HttpServletRequest request,
            HttpServletResponse response) {

        StreamingResponseBody streamingOutput = output -> {
            Map<String, ConflictResolution> conflictResolutions = ConflictUtils
                .getResolutionsFromSession(request.getSession());
            ConflictResolution conflictResolution = conflictResolutions.get(name);
            try (InputStream input = conflictResolution.getCustomResolutionFile().getInput()) {
                IOUtils.copy(input, output);
            } finally {
                output.flush();
            }
        };

        String cookieName = Constants.RESPONSE_MONITOR_COOKIE + "_" + cookieId;
        return prepareResponse(request, response, cookieName, name, streamingOutput);
    }

    private ResponseEntity<?> prepareResponse(HttpServletRequest request,
            HttpServletResponse response,
            String cookieName,
            String filePath,
            StreamingResponseBody streamingOutput) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            streamingOutput.writeTo(output);
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            response.addCookie(newCookie(cookieName, "success", request.getContextPath()));
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, WebTool.getContentDispositionValue(fileName))
                .body(output.toByteArray());
        } catch (IOException e) {
            String message = "Failed to download file.";
            LOG.error(message, e);
            response.addCookie(newCookie(cookieName, message, request.getContextPath()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    private static Cookie newCookie(String cookieName, String value, String contextPath) {
        if (StringUtils.isEmpty(contextPath)) {
            contextPath = "/"; // //EPBDS-7613
        }
        var cookie = new Cookie(cookieName, StringTool.encodeURL(value));
        cookie.setPath(contextPath);
        cookie.setVersion(1);
        cookie.setMaxAge(-1);
        cookie.setSecure(false);
        cookie.setHttpOnly(false); // Has to be visible from client scripting
        return cookie;
    }

    private WorkspaceUserImpl getUser() {
        return new WorkspaceUserImpl(getUserName(),
            (username) -> Optional.ofNullable(userManagementService.getUser(username))
                .map(usr -> new UserInfo(usr.getUsername(), usr.getEmail(), usr.getDisplayName()))
                .orElse(null));
    }

    private static String getUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

}
