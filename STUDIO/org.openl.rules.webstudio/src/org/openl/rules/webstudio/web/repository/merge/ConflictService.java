package org.openl.rules.webstudio.web.repository.merge;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.openl.rules.repository.api.FileItem;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.IOUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Path("/conflict/")
public class ConflictService {
    private static final Logger LOG = LoggerFactory.getLogger(ConflictService.class);

    private final MultiUserWorkspaceManager workspaceManager;

    public ConflictService(MultiUserWorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @GET
    @Path("repository")
    @Produces("application/octet-stream")
    public Response repository(@QueryParam(Constants.REQUEST_PARAM_NAME) final String name,
            @QueryParam(Constants.REQUEST_PARAM_VERSION) final String version,
            @QueryParam(Constants.RESPONSE_MONITOR_COOKIE) String cookieId,
            @Context HttpServletRequest request) {

        String cookieName = Constants.RESPONSE_MONITOR_COOKIE + "_" + cookieId;
        StreamingOutput streamingOutput = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException {
                InputStream stream = null;
                try {
                    FileItem file = workspaceManager.getUserWorkspace(getUser())
                        .getDesignTimeRepository()
                        .getRepository()
                        .readHistory(name, version);
                    if (file == null) {
                        throw new FileNotFoundException("File '" + name + "' is not found");
                    }

                    stream = file.getStream();
                    IOUtils.copy(stream, output);
                    output.flush();
                } catch (WorkspaceException e) {
                    LOG.warn(e.getMessage(), e);
                    throw new IOException(e.getMessage(), e);
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }
        };

        return prepareResponse(request, cookieName, name, streamingOutput);
    }

    @GET
    @Path("local")
    @Produces("application/octet-stream")
    public Response local(@QueryParam(Constants.REQUEST_PARAM_NAME) final String name,
            @QueryParam(Constants.RESPONSE_MONITOR_COOKIE) String cookieId,
            @Context HttpServletRequest request) {

        String cookieName = Constants.RESPONSE_MONITOR_COOKIE + "_" + cookieId;
        StreamingOutput streamingOutput = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException {
                InputStream stream = null;
                try {
                    UserWorkspace userWorkspace = workspaceManager.getUserWorkspace(getUser());
                    String rulesLocation = userWorkspace.getDesignTimeRepository().getRulesLocation();
                    String localName = name.substring(rulesLocation.length());
                    FileItem file = userWorkspace.getLocalWorkspace().getRepository().read(localName);
                    if (file == null) {
                        throw new FileNotFoundException("File " + localName + " is not found");
                    }
                    stream = file.getStream();
                    IOUtils.copy(stream, output);
                    output.flush();
                } catch (WorkspaceException e) {
                    LOG.warn(e.getMessage(), e);
                    throw new IOException(e.getMessage(), e);
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }
        };

        return prepareResponse(request, cookieName, name, streamingOutput);
    }

    @GET
    @Path("merged")
    @Produces("application/octet-stream")
    public Response merged(@QueryParam(Constants.REQUEST_PARAM_NAME) final String name,
            @QueryParam(Constants.RESPONSE_MONITOR_COOKIE) String cookieId,
            @Context HttpServletRequest request) {

        StreamingOutput streamingOutput = output -> {
            Map<String, ConflictResolution> conflictResolutions = ConflictUtils
                .getConflictsFromSession(request.getSession());
            ConflictResolution conflictResolution = conflictResolutions.get(name);
            try (InputStream input = conflictResolution.getCustomResolutionFile().getInput()) {
                IOUtils.copy(input, output);
            } finally {
                output.flush();
            }
        };

        String cookieName = Constants.RESPONSE_MONITOR_COOKIE + "_" + cookieId;
        return prepareResponse(request, cookieName, name, streamingOutput);
    }

    private Response prepareResponse(HttpServletRequest request,
            String cookieName,
            String filePath,
            StreamingOutput streamingOutput) {
        try {
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            return Response.ok(streamingOutput)
                .cookie(newCookie(cookieName, "success", request.getContextPath()))
                .header("Content-Disposition", "attachment;filename=" + fileName)
                .build();
        } catch (Exception e) {
            String message = "Failed to download file.";
            LOG.error(message, e);

            return Response.status(Response.Status.NOT_FOUND)
                .entity(e.getMessage())
                .cookie(newCookie(cookieName, message, request.getContextPath()))
                .build();
        }
    }

    private NewCookie newCookie(String cookieName, String value, String contextPath) {
        if (StringUtils.isEmpty(contextPath)) {
            contextPath = "/"; // //EPBDS-7613
        }

        return new NewCookie(cookieName,
            StringTool.encodeURL(value),
            contextPath,
            null,
            1,
            null,
            -1,
            null,
            false,
            false);
    }

    private WorkspaceUserImpl getUser() {
        String name = getUserName();
        return new WorkspaceUserImpl(name);
    }

    private String getUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

}