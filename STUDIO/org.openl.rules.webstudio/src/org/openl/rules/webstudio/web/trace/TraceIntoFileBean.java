package org.openl.rules.webstudio.web.trace;

import org.apache.commons.lang3.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.commons.web.util.WebTool;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.IOUtils;
import org.openl.vm.trace.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * Request scope managed bean for Trace into File functionality.
 */
public class TraceIntoFileBean {

    private final Logger log = LoggerFactory.getLogger(TraceIntoFileBean.class);

    public static final String EXTENSION_SEPARATOR = ".";

    /**
     * Output file name without extension. By default 'trace'.
     */
    private String fileBaseName = "trace";

    /**
     * Output file format.
     */
    private String fileFormat = TraceFormatterFactory.FORMAT_TEXT;

    public void traceIntoFile(TestSuite testSuite) {
        ProjectModel model = WebStudioUtils.getProjectModel();
        ITracerObject tracer = model.traceElement(testSuite);

        TracePrinter tracePrinter = getTracePrinter(fileFormat);

        HttpServletResponse response = (HttpServletResponse) FacesUtils.getResponse();
        initResponse(response, getFileName());

        Writer writer = null;

        try {
            writer = response.getWriter();
            tracePrinter.print(tracer, writer);
            writer.close();
        } catch (IOException e) {
            log.error("Error when printing trace", e);
        } finally {
            IOUtils.closeQuietly(writer);
        }

        FacesUtils.getFacesContext().responseComplete();
    }

    private TracePrinter getTracePrinter(String fileFormat) {
        DefaultTracePrinter tracePrinter = new DefaultTracePrinter();

        TraceFormatter traceFormatter = new TraceFormatterFactory().getTraceFormatter(fileFormat);
        tracePrinter.setFormatter(traceFormatter);

        return tracePrinter;
    }

    private void initResponse(HttpServletResponse response, String outputFileName) {
        WebTool.setContentDisposition(response, outputFileName);

        String contentType = new MimetypesFileTypeMap().getContentType(outputFileName);
        response.setContentType(contentType);
    }

    private String getFileName() {
        StringBuilder result = new StringBuilder();

        result.append(fileBaseName);

        if (StringUtils.isNotBlank(fileFormat)) {
            result.append(EXTENSION_SEPARATOR).append(fileFormat);
        }

        return result.toString();
    }
}
