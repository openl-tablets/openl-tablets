package org.openl.rules.webstudio.web.test;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@Path("/test/")
public class TestDownloadService {
    private final Logger log = LoggerFactory.getLogger(TestDownloadService.class);

    @GET
    @Path("download")
    @Produces("application/zip")
    public Response download(@QueryParam(Constants.REQUEST_PARAM_ID) String id,
            @QueryParam(Constants.REQUEST_PARAM_TEST_RANGES) String testRanges,
            @QueryParam(Constants.REQUEST_PARAM_PERPAGE) Integer pp,
            @QueryParam(Constants.RESPONSE_MONITOR_COOKIE) String cookieId,
            @Context HttpServletRequest request) {

        HttpSession session = request.getSession();

        final int testsPerPage = pp != null ? pp : WebStudioUtils.getWebStudio(session).getTestsPerPage();
        final TestUnitsResults[] results = Utils.runTests(id, testRanges, session);

        String cookieName = Constants.RESPONSE_MONITOR_COOKIE + "_" + cookieId;
        try {
            StreamingOutput streamingOutput = new StreamingOutput() {
                @Override
                public void write(OutputStream output) throws IOException {
                    new TestResultExport().export(results, testsPerPage, output);
                }
            };
            return Response.ok(streamingOutput, "application/xlsx")
                    .cookie(newCookie(cookieName, "success", request.getContextPath()))
                    .header("Content-Disposition", "attachment;filename=test-results.xlsx")
                    .build();
        } catch (Exception e) {
            String message = "Failed to export results.";
            log.error(message, e);

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

}