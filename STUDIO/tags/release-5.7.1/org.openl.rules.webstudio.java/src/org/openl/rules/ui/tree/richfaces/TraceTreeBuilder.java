package org.openl.rules.ui.tree.richfaces;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.util.tree.ITreeElement;

public class TraceTreeBuilder extends TreeBuilder {

    private TraceHelper traceHelper;

    public TraceTreeBuilder(ITreeElement<?> root, TraceHelper traceHelper) {
        super(root);
        this.traceHelper = traceHelper;
    }

    @Override
    protected String getUrl(ITreeElement<?> element) {
        return FacesUtils.getContextPath() + "/jsp/showTraceTable.jsp?" + Constants.REQUEST_PARAM_ID + "="
            + traceHelper.getNodeKey(element);
    }

}
