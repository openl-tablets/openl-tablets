package org.openl.rules.webstudio.web.trace;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.vm.trace.DefaultTracePrinter;
import org.openl.vm.trace.TraceFormatter;
import org.openl.vm.trace.TraceFormatterFactory;
import org.openl.vm.trace.TracePrinter;
import org.openl.vm.trace.Tracer;

/**
 * Request scope managed bean for Trace into File functionality.
 */
public class TraceIntoFileBean {

    private final Log LOG = LogFactory.getLog(TraceIntoFileBean.class);

    public static final String EXTENSION_SEPARATOR = ".";

    /**
     * Uri of table to trace.
     */
    private String tableUri;

    /**
     * Output file name without extension. By default 'trace'.
     */
    private String fileBaseName = "trace";

    /**
     * Output file format (extension).
     */
    private String fileFormat;

    public String traceIntoFile() {
        Tracer tracer = trace(tableUri);

        TracePrinter tracePrinter = getTracePrinter(fileFormat);

        HttpServletResponse response = (HttpServletResponse) FacesUtils.getResponse();
        initResponse(response, getFileName());

        Writer writer = null;

        try {
            writer = response.getWriter();
            tracePrinter.print(tracer, writer);
            writer.close();
        } catch (IOException e) {
            LOG.error("Error when printing trace", e);
        } finally {
            IOUtils.closeQuietly(writer);
        }

        FacesUtils.getFacesContext().responseComplete();

        return null;
    }

    private Tracer trace(String tableUri) {
        ProjectModel model = WebStudioUtils.getProjectModel();
        return model.traceElement(tableUri, null, null);
    }

    private TracePrinter getTracePrinter(String fileFormat) {
        DefaultTracePrinter tracePrinter = new DefaultTracePrinter();

        TraceFormatter traceFormatter = new TraceFormatterFactory().getTraceFormatter(fileFormat);
        tracePrinter.setFormatter(traceFormatter);

        return tracePrinter;
    }

    private void initResponse(HttpServletResponse response, String outputFileName) {
        response.setHeader("Content-disposition", "attachment; filename=" + outputFileName);

        String contentType = new MimetypesFileTypeMap().getContentType(outputFileName);
        response.setContentType(contentType);
    }

    public String getTableUri() {
        return tableUri;
    }

    public void setTableUri(String tableUri) {
        this.tableUri = tableUri;
    }

    public String getFileBaseName() {
        return fileBaseName;
    }

    public void setFileBaseName(String fileBaseName) {
        this.fileBaseName = fileBaseName;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getFileName() {
        StringBuilder result = new StringBuilder();

        result.append(fileBaseName);

        if (StringUtils.isNotBlank(fileFormat)) {
            result.append(EXTENSION_SEPARATOR).append(fileFormat);
        }

        return result.toString();
    }

    public List<SelectItem> getFileTypes() {
        List<SelectItem> fileTypes = new ArrayList<SelectItem>();
        fileTypes.add(new SelectItem(TraceFormatterFactory.FORMAT_TEXT, "Text"));
        return fileTypes;
    }

}
