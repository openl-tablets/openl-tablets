package org.openl.rules.webstudio.web.diff;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.openl.util.FileTool;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@Path("/public/compare/")
public class DiffService {
    private static final Logger LOG = LoggerFactory.getLogger(DiffService.class);

    private final DiffManager diffManager;

    public DiffService(DiffManager diffManager) {
        this.diffManager = diffManager;
    }

    @POST
    @Path("xls")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response compareXls(@Context UriInfo uriInfo,
            @Multipart(value = "file1") InputStream file1,
            @Multipart(value = "file2") InputStream file2) {
        try {
            String requestId = UUID.randomUUID().toString();

            File excelFile1 = createTempFile(file1, "file1");
            File excelFile2 = createTempFile(file2, "file2");
            diffManager.add(requestId, new ShowDiffController(excelFile1, excelFile2));

            String root = uriInfo.getBaseUri().toString();
            if (root.endsWith("/web") || root.endsWith("/rest")) {
                // Remove prefix for rest service because we return a link to a html page.
                root = root.substring(0, root.lastIndexOf('/'));
            }
            return Response.seeOther(new URI(root + "/faces/pages/public/showDiff.xhtml?id=" + requestId)).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    private File createTempFile(InputStream inputStream, String fullName) throws FileNotFoundException {
        if (inputStream == null) {
            return null;
        }
        File tempFile = FileTool.toTempFile(inputStream, FileUtils.getName(fullName));
        if (tempFile == null) {
            throw new FileNotFoundException(String.format("Cannot create temp file for '%s'", fullName));
        }
        return tempFile;
    }

}