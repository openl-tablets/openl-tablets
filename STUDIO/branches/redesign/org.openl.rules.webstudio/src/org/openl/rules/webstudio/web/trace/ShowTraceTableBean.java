package org.openl.rules.webstudio.web.trace;

import java.util.Collections;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.ITableTracerObject;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.testmethod.ExecutionParamDescription;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * Request scope managed bean for showTraceTable page.
 */
@ManagedBean
@RequestScoped
public class ShowTraceTableBean {

    private TraceHelper traceHelper;
    private int traceElementId;

    private String tracerUri;
    private String tracerName;

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
    }

    public String getTracerUri() {
        return tracerUri;
    }

    public String getTracerName() {
        return tracerName;
    }

    public IOpenLTable getTraceTable() {
        return traceHelper.getTraceTable(traceElementId);
    }

    public IGridFilter getTraceFilter() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        return traceHelper.makeFilter(traceElementId, model);
    }

    public String getTraceTableView() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        return model.getTableView(FacesUtils.getRequestParameter("view"));
    }
    
    public ExecutionParamDescription[] getInputParameters() {
        ITableTracerObject tto = traceHelper.getTableTracer(traceElementId);
        return new TracerObjectDecorator(tto).getInputParameters();
    }
    
    public ExecutionParamDescription[] getReturnResult() {
        return new ExecutionParamDescription[]{new TracerObjectDecorator(traceHelper.getTableTracer(traceElementId)).getReturnResult()};
    }
    
    public String getFormattedResult() {
        return new TracerObjectDecorator(traceHelper.getTableTracer(traceElementId)).getFormattedResult();
    }
    
    public boolean getSpreadsheetResultReturn() {        
        return new TracerObjectDecorator(traceHelper.getTableTracer(traceElementId)).isSpreadsheetResult();
    }

    public List<OpenLMessage> getErrors() {
        Throwable error = traceHelper.getTracerError(traceElementId);

        if (error != null) {
            Throwable cause = error.getCause();
            if (cause != null) {
                return OpenLMessagesUtils.newMessages(cause);
            }
            return OpenLMessagesUtils.newMessages(error);
        }

        return Collections.emptyList();
    }

}
