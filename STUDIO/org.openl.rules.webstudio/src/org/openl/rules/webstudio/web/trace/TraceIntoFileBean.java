package org.openl.rules.webstudio.web.trace;

import java.io.IOException;
import java.io.Writer;

import javax.activation.MimetypesFileTypeMap;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.commons.web.util.WebTool;
import org.openl.rules.webstudio.web.test.RunTestHelper;
import org.openl.util.IOUtils;
import org.openl.vm.trace.ITracerObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request scope managed bean for Trace into File functionality.
 */
@SessionScoped
@ManagedBean
public class TraceIntoFileBean {

    private final Logger log = LoggerFactory.getLogger(TraceIntoFileBean.class);

    @ManagedProperty("#{runTestHelper}")
    private RunTestHelper runTestHelper;

    public void setRunTestHelper(RunTestHelper runTestHelper) {
        this.runTestHelper = runTestHelper;
    }

    public static final String EXTENSION_SEPARATOR = ".";

    /**
     * Output file name without extension. By default 'trace'.
     */
    private final String fileBaseName = "trace";

    /**
     * Output file format.
     */
    private String fileFormat = TraceFormatterFactory.FORMAT_TEXT;

    public void traceIntoFile() {
        ITracerObject tracer = runTestHelper.getTraceObject();

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
