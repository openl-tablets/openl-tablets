package org.openl.rules.webstudio.web.trace;

import java.io.IOException;
import java.io.Writer;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.openl.rules.webstudio.util.WebTool;
import org.openl.main.SourceCodeURLConstants;
import org.openl.rules.webstudio.web.test.RunTestHelper;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request scope managed bean for Trace into File functionality.
 *
 * @author Yury Molchan
 */
@SessionScoped
@ManagedBean
public class TraceIntoFileBean {
    private static final char[] indents = new char[256];

    static {
        for (int i = 0; i < indents.length; i++) {
            indents[i] = '\t';
        }
    }

    private final Logger log = LoggerFactory.getLogger(TraceIntoFileBean.class);

    @ManagedProperty("#{runTestHelper}")
    private RunTestHelper runTestHelper;

    public void setRunTestHelper(RunTestHelper runTestHelper) {
        this.runTestHelper = runTestHelper;
    }

    public void traceIntoFile() {
        ITracerObject tracer = runTestHelper.getTraceObject();

        HttpServletResponse response = (HttpServletResponse) (ServletResponse) WebStudioUtils.getExternalContext()
            .getResponse();

        String outputFileName = "trace.txt";
        response.setHeader("Content-Disposition", WebTool.getContentDispositionValue(outputFileName));

        response.setContentType("text/plain");

        Writer writer = null;

        try {
            writer = response.getWriter();
            print(tracer, 0, writer);
            writer.close();
        } catch (IOException e) {
            log.error("Error when printing trace", e);
        } finally {
            IOUtils.closeQuietly(writer);
        }

        FacesContext.getCurrentInstance().responseComplete();
    }

    private void print(ITracerObject tracer, int level, Writer writer) throws IOException {

        Iterable<ITracerObject> tracerObjects = tracer.getChildren();
        for (ITracerObject aTrace : tracerObjects) {
            writer.write(indents, 0, level % indents.length);
            writer.write("TRACE: ");
            writer.write(TraceFormatter.getDisplayName(aTrace));
            writer.write('\n');
            writer.write(indents, 0, level % indents.length);
            writer.write(SourceCodeURLConstants.AT_PREFIX);
            writer.write(FileUtils.getBaseName(aTrace.getUri()));
            writer.write('&');
            writer.write(SourceCodeURLConstants.OPENL);
            writer.write('=');
            writer.write('\n');

            print(aTrace, level + 1, writer);
        }
    }
}
