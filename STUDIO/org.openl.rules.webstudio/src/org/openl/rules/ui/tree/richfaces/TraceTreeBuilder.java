package org.openl.rules.ui.tree.richfaces;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.dt.trace.DTConditionTraceObject;
import org.openl.rules.dt.trace.DecisionTableTraceObject;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.util.tree.ITreeElement;

public class TraceTreeBuilder extends TreeBuilder {
    private static final int UNSUCCESSFUL = 0;
    private static final int SUCCESSFUL_WITHOUT_RESULT = 1;
    private static final int SUCCESSFUL_WITH_RESULT = 2;

    private TraceHelper traceHelper;

    public TraceTreeBuilder(ITreeElement<?> root, TraceHelper traceHelper) {
        super(root, false);
        this.traceHelper = traceHelper;
    }

    @Override
    String getUrl(ITreeElement<?> element) {
        Integer nodeKey = traceHelper.getNodeKey(element);
        String params = nodeKey != null ? Constants.REQUEST_PARAM_ID + "=" + nodeKey : "";
        return FacesUtils.getContextPath() + "/faces/pages/modules/trace/showTraceTable.xhtml?" + params;
    }

    @Override
    int getState(ITreeElement<?> element) {
        if (element instanceof DTConditionTraceObject) {
            DTConditionTraceObject condition = (DTConditionTraceObject) element;
            return condition.isSuccessful() ? (condition.hasRuleResult() ? SUCCESSFUL_WITH_RESULT : SUCCESSFUL_WITHOUT_RESULT) : UNSUCCESSFUL;
        }

        return super.getState(element);
    }

    @Override
    Iterable<? extends ITreeElement<?>> getChildrenIterator(ITreeElement<?> source) {
        if (source instanceof DecisionTableTraceObject) {
            DecisionTableTraceObject parent = (DecisionTableTraceObject) source;
            return traceHelper.isDetailedTraceTree() ? parent.getChildren() : parent.getTraceResults();
        }

        return super.getChildrenIterator(source);
    }
}
