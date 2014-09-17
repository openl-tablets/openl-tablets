package org.openl.rules.webstudio.web.trace;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.result.SpreadsheetResultHelper;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.ITableTracerObject;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.ui.ObjectViewer;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IParameterDeclaration;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import java.util.Collections;
import java.util.List;

/**
 * Request scope managed bean for showTraceTable page.
 */
@ManagedBean
@RequestScoped
public class ShowTraceTableBean {

    private TraceHelper traceHelper;
    private int traceElementId;

    public ShowTraceTableBean() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        traceHelper = studio.getTraceHelper();

        String traceElementIdParam = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);
        traceElementId = -100;
        if (traceElementIdParam != null) {
            traceElementId = Integer.parseInt(traceElementIdParam);
        }
    }

    public IOpenLTable getTraceTable() {
        return traceHelper.getTraceTable(traceElementId);
    }

    public IGridFilter[] getTraceFilters() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        return traceHelper.makeFilters(traceElementId, model);
    }

    public ParameterWithValueDeclaration[] getInputParameters() {
        ITableTracerObject tto = traceHelper.getTableTracer(traceElementId);
        ATableTracerNode tracerNode = null;
        if (tto instanceof ATableTracerNode) {
            tracerNode = (ATableTracerNode) tto;
        } else if (tto != null && tto.getParent() instanceof ATableTracerNode) {
            // ATableTracerLeaf
            tracerNode = (ATableTracerNode) tto.getParent();
        }
        if (tracerNode == null || !(tracerNode.getTraceObject() instanceof ExecutableRulesMethod)) {
            return null;
        }

        ExecutableRulesMethod tracedMethod = (ExecutableRulesMethod) tracerNode.getTraceObject();
        Object[] parameters = tracerNode.getParameters();
        ParameterWithValueDeclaration[] paramDescriptions = new ParameterWithValueDeclaration[parameters.length];
        for (int i = 0; i < paramDescriptions.length; i++) {
            paramDescriptions[i] = new ParameterWithValueDeclaration(tracedMethod.getSignature().getParameterName(i),
                    parameters[i], IParameterDeclaration.IN);
        }
        return paramDescriptions;
    }

    public ParameterWithValueDeclaration[] getReturnResult() {
        ITableTracerObject tableTracer = traceHelper.getTableTracer(traceElementId);
        ParameterWithValueDeclaration returnResult = null;
        Object result = tableTracer.getResult();
        if (result != null) {
            returnResult = new ParameterWithValueDeclaration("return", result, IParameterDeclaration.OUT);
        }
        return new ParameterWithValueDeclaration[]{returnResult};
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

    public static boolean isSpreadsheetResult(Object value) {
        return value != null && SpreadsheetResultHelper.isSpreadsheetResult(value.getClass());
    }

    public String getFormattedSpreadsheetResult(Object value) {
        return ObjectViewer.displaySpreadsheetResultNoFilters((SpreadsheetResult) value);
    }
}
