package org.openl.rules.ui.tree.richfaces;

import java.util.Arrays;
import java.util.Collections;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.dtx.trace.DTConditionTraceObject;
import org.openl.rules.dtx.trace.DTRuleTracerLeaf;
import org.openl.rules.dtx.trace.DecisionTableTraceObject;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.tree.ITreeElement;

public class TraceTreeBuilder extends TreeBuilder {

    private final boolean detailedTraceTree;

    public TraceTreeBuilder(boolean detailedTraceTree) {
        this.detailedTraceTree = detailedTraceTree;
    }

    @Override
    String getUrl(ITreeElement<?> element) {
        TraceHelper traceHelper = WebStudioUtils.getTraceHelper();
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
            } else{
                ITreeElement<?> result = findResult(element.getChildren());
                if (result != null) {
                    return type + "_result";
                }
            }
        }
        return type;
    }

    @SuppressWarnings("unchecked")
	@Override
    Iterable<? extends ITreeElement<?>> getChildrenIterator(ITreeElement<?> source) {
        if (source instanceof DecisionTableTraceObject && !detailedTraceTree) {
            ITreeElement<?> resultNode = findResult(source.getChildren());
            return resultNode == null ? Collections.EMPTY_LIST : Arrays.asList(resultNode);
        }

        return super.getChildrenIterator(source);
    }

    private ITreeElement<?> findResult(Iterable<? extends ITreeElement<?>> children) {
        for (ITreeElement<?> child : children) {
            if (child instanceof DTRuleTracerLeaf) {
                return child;
            }
            ITreeElement<?> result = findResult(child.getChildren());
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
