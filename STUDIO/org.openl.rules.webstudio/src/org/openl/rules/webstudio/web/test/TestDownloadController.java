package org.openl.rules.webstudio.web.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/test")
@Tag(name = "Test")
public class TestDownloadController {

    private static final Logger LOG = LoggerFactory.getLogger(TestDownloadController.class);

    @Operation(summary = "test.download.summary", description = "test.download.summary")
    @GetMapping(value = "/testcase")
    @ApiResponse(responseCode = "200", description = "OK", headers = {
            @Header(name = HttpHeaders.CONTENT_DISPOSITION, description = "header.content-disposition.desc", required = true),
            @Header(name = HttpHeaders.SET_COOKIE, description = "header.set-cookie.desc") }, content = @Content(mediaType = "application/xlsx", schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<?> download(
            @Parameter(description = "test.field.test-table-id") @RequestParam(value = Constants.REQUEST_PARAM_ID, required = false) String id,
            @Parameter(description = "test.field.test-range") @RequestParam(value = Constants.REQUEST_PARAM_TEST_RANGES, required = false) String testRanges,
            @Parameter(description = "test.field.per-page") @RequestParam(value = Constants.REQUEST_PARAM_PERPAGE, required = false) Integer pp,
            @RequestParam(Constants.RESPONSE_MONITOR_COOKIE) String cookieId,
            @RequestParam(Constants.REQUEST_PARAM_CURRENT_OPENED_MODULE) Boolean currentOpenedModule,
            HttpServletRequest request,
            HttpServletResponse response) {

        HttpSession session = request.getSession();

        final int testsPerPage = pp != null ? pp : WebStudioUtils.getWebStudio(session).getTestsPerPage();
        final TestUnitsResults[] results = Utils.runTests(id, testRanges, currentOpenedModule, session);

        String cookieName = Constants.RESPONSE_MONITOR_COOKIE + "_" + cookieId;
        StreamingResponseBody streamingOutput = output -> new TestResultExport().export(output, testsPerPage, results);

        return prepareResponse(request, response, cookieName, streamingOutput);
    }

    private ResponseEntity<?> prepareResponse(HttpServletRequest request,
            HttpServletResponse response,
            String cookieName,
            StreamingResponseBody streamingOutput) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            streamingOutput.writeTo(output);
            response.addCookie(newCookie(cookieName, "success", request.getContextPath()));
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=test-results.xlsx")
                .header(HttpHeaders.CONTENT_TYPE, "application/xlsx")
                .body(output.toByteArray());
        } catch (IOException e) {
            String message = "Failed to export results.";
            LOG.error(message, e);
            response.addCookie(newCookie(cookieName, message, request.getContextPath()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(summary = "test.manual.summary", description = "test.manual.summary")
    @GetMapping(value = "/rule")
    @ApiResponse(responseCode = "200", description = "OK", headers = {
            @Header(name = HttpHeaders.CONTENT_DISPOSITION, description = "header.content-disposition.desc", required = true),
            @Header(name = HttpHeaders.SET_COOKIE, description = "header.set-cookie.desc") }, content = @Content(mediaType = "application/xlsx", schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<?> manual(@RequestParam(Constants.RESPONSE_MONITOR_COOKIE) String cookieId,
            @RequestParam(Constants.REQUEST_PARAM_CURRENT_OPENED_MODULE) Boolean currentOpenedModule,
            HttpServletRequest request,
            HttpServletResponse response) {
        HttpSession session = request.getSession();
        String cookieName = Constants.RESPONSE_MONITOR_COOKIE + "_" + cookieId;

        ProjectModel model = WebStudioUtils.getWebStudio(session).getModel();
        TestSuite testSuite = Utils.pollTestFromSession(session);
        if (testSuite != null) {
            final TestUnitsResults results = model.runTest(testSuite, currentOpenedModule);
            StreamingResponseBody streamingOutput = output -> new RulesResultExport().export(output, -1, results);
            return prepareResponse(request, response, cookieName, streamingOutput);
        }

        String failure = "Test data is not available anymore";
        response.addCookie(newCookie(cookieName, failure, request.getContextPath()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Input parameters not found");
    }

    private Cookie newCookie(String cookieName, String value, String contextPath) {
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

}