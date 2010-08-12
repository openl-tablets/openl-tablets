package org.openl.rules.webstudio.web.trace;

import java.util.Collections;
import java.util.List;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * Request scope managed bean for showTraceTable page.
 */
public class ShowTraceTableBean {

    private TraceHelper traceHelper;
    private int traceElementId;

    private String tracerUri;
    private String tracerName;
    private String tracerHeader;

    public ShowTraceTableBean() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        traceHelper = studio.getTraceHelper();

        String traceElementIdParam = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);
        traceElementId = -100;
        if (traceElementIdParam != null) {
            traceElementId = Integer.parseInt(traceElementIdParam);
        }

        tracerUri = traceHelper.getTracerUri(traceElementId);
        tracerName = traceHelper.getTracerName(traceElementId);
        tracerHeader = traceHelper.getTracerHeader(traceElementId);
    }

    public String getTracerUri() {
        return tracerUri;
    }

    public String getTracerName() {
        return tracerName;
    }

    public String getTracerHeader() {
        return tracerHeader;
    }

    public String getTraceTable() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        String view = studio.getModel().getTableView(FacesUtils.getRequestParameter("view"));
        return traceHelper.showTrace(traceElementId, studio.getModel(), view);
    }

    public List<OpenLMessage> getErrors() {
        Throwable error = traceHelper.getTracerError(traceElementId);

        if (error != null) {
            return OpenLMessagesUtils.newMessages(error.getCause());
        }

        return Collections.emptyList();
    }

}
