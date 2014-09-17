package org.openl.rules.ui.tree.richfaces;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.dt.trace.DTConditionTraceObject;
import org.openl.rules.dt.trace.DecisionTableTraceObject;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.tree.ITreeElement;

public class TraceTreeBuilder extends TreeBuilder {

    @Override
    String getUrl(ITreeElement<?> element) {
        TraceHelper traceHelper = WebStudioUtils.getWebStudio().getTraceHelper();
        Integer nodeKey = traceHelper.getNodeKey(element);
        String params = nodeKey != null ? Constants.REQUEST_PARAM_ID + "=" + nodeKey : "";
        return FacesUtils.getContextPath() + "/faces/pages/modules/trace/showTraceTable.xhtml?" + params;
    }


    @Override
    String getType(ITreeElement<?> element) {
        String type = super.getType(element);
        if (element instanceof DTConditionTraceObject) {
            DTConditionTraceObject condition = (DTConditionTraceObject) element;
            if (!condition.isSuccessful()) {
                return type + "_fail";
            } else if (condition.hasRuleResult()) {
                return type + "_result";
            }
        }
        return type;
    }

    @Override
    Iterable<? extends ITreeElement<?>> getChildrenIterator(ITreeElement<?> source) {
        if (source instanceof DecisionTableTraceObject) {
            DecisionTableTraceObject parent = (DecisionTableTraceObject) source;
            TraceHelper traceHelper = WebStudioUtils.getWebStudio().getTraceHelper();
            return traceHelper.isDetailedTraceTree() ? parent.getChildren() : parent.getTraceResults();
        }

        return super.getChildrenIterator(source);
    }
}
