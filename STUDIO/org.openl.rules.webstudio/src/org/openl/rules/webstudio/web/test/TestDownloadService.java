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

import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.export.RulesResultExport;
import org.openl.rules.testmethod.export.TestResultExport;
import org.openl.rules.ui.ProjectModel;
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
    private static final Logger LOG = LoggerFactory.getLogger(TestDownloadService.class);

    @GET
    @Path("testcase")
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
        StreamingOutput streamingOutput = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException {
                new TestResultExport().export(output, testsPerPage, results);
            }
        };

        return prepareResponse(request, cookieName, streamingOutput);
    }

    private Response prepareResponse(@Context HttpServletRequest request,
            String cookieName,
            StreamingOutput streamingOutput) {
        try {
            return Response.ok(streamingOutput, "application/xlsx")
                .cookie(newCookie(cookieName, "success", request.getContextPath()))
                .header("Content-Disposition", "attachment;filename=test-results.xlsx")
                .build();
        } catch (Exception e) {
            String message = "Failed to export results.";
            LOG.error(message, e);

            return Response.status(Response.Status.NOT_FOUND)
                .entity(e.getMessage())
                .cookie(newCookie(cookieName, message, request.getContextPath()))
                .build();
        }
    }

    @GET
    @Path("rule")
    @Produces("application/zip")
    public Response manual(@QueryParam(Constants.REQUEST_PARAM_ID) String id,
            @QueryParam(Constants.RESPONSE_MONITOR_COOKIE) String cookieId,
            @Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        String cookieName = Constants.RESPONSE_MONITOR_COOKIE + "_" + cookieId;

        ProjectModel model = WebStudioUtils.getWebStudio(session).getModel();
        TestSuite testSuite = Utils.pollTestFromSession(session);
        if (testSuite != null) {
            final TestUnitsResults results = model.runTest(testSuite);
            StreamingOutput streamingOutput = new StreamingOutput() {
                @Override
                public void write(OutputStream output) throws IOException {
                    new RulesResultExport().export(output, -1, results);
                }
            };
            return prepareResponse(request, cookieName, streamingOutput);
        }

        String failure = "Test data isn't available anymore";
        return Response.status(Response.Status.NOT_FOUND)
            .entity("Input parameters not found")
            .cookie(newCookie(cookieName, failure, request.getContextPath()))
            .build();
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