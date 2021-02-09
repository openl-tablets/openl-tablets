package org.openl.rules.webstudio.web.trace;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.openl.rules.webstudio.web.test.RunTestHelper;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.rules.webstudio.web.trace.node.RefToTracerNodeObject;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Request scope managed bean for Trace into File functionality.
 *
 * @author Yury Molchan
 */
@Service
@SessionScope
public class TraceIntoFileBean {

    private static final int MAX_WAIT_TIMEOUT = 60 * 1000;

    private static final char[] indents = new char[256];

    static {
        Arrays.fill(indents, '\t');
    }

    private final RunTestHelper runTestHelper;

    public TraceIntoFileBean(RunTestHelper runTestHelper) {
        this.runTestHelper = runTestHelper;
    }

    public void traceIntoFile() throws IOException {
        ITracerObject tracer = runTestHelper.getTraceObject();

        HttpServletResponse response = (HttpServletResponse) WebStudioUtils.getExternalContext().getResponse();
        response.setHeader("Content-Disposition", "attachment; filename=trace.txt; filename*=UTF-8''trace.txt");
        response.setContentType("text/plain");

        try (Writer writer = response.getWriter()) {
            long start = System.currentTimeMillis();
            try {
                print(tracer, 0, writer, start + MAX_WAIT_TIMEOUT);
            } catch (TimeoutException e) {
                writer.write("\n!!!TRACE WAS LIMITED BY TIMEOUT!!!\n");
            }
        } finally {
            FacesContext.getCurrentInstance().responseComplete();
        }
    }

    private void print(ITracerObject tracer, int level, Writer writer, long deadline) throws IOException,
                                                                                      TimeoutException {
        if (deadline < System.currentTimeMillis()) {
            throw new TimeoutException();
        }
        Iterable<ITracerObject> tracerObjects = tracer.getChildren();
        for (ITracerObject aTrace : tracerObjects) {
            writer.write(indents, 0, level % indents.length);
            writer.write("TRACE: ");
            writer.write(TraceFormatter.getDisplayName(aTrace));
            writer.write('\n');
            writer.write(indents, 0, level % indents.length);
            writer.write("    at ");
            writer.write(FileUtils.getBaseName(aTrace.getUri()));
            writer.write("&openl=");
            writer.write('\n');

            if (aTrace instanceof RefToTracerNodeObject) {
                continue;
            }
            print(aTrace, level + 1, writer, deadline);
        }
    }

}
