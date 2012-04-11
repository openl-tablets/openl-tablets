package org.openl.rules.ui.tree;

import org.openl.rules.ui.TraceHelper;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.util.tree.ITreeElement;

public class TraceRichFacesTreeBuilder extends RichFacesTreeBuilder {

    private TraceHelper traceHelper;

    public TraceRichFacesTreeBuilder(ITreeElement<?> root, TraceHelper traceHelper) {
        super(root);
        this.traceHelper = traceHelper;
    }

    @Override
    protected String getUrl(ITreeElement<?> element) {
        return FacesUtils.getContextPath() + "/jsp/showTraceTable.jsp?" + Constants.REQUEST_PARAM_ID + "="
            + traceHelper.getNodeKey(element);
    }

}
