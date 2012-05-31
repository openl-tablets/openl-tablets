package org.openl.rules.ui.tree.richfaces;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.util.tree.ITreeElement;
import org.openl.rules.dt.trace.*;

public class TraceTreeBuilder extends TreeBuilder {
    private static final int SUCCESSFUL = 1;
    private static final int UNSUCCESSFUL = 2;

    private TraceHelper traceHelper;

    public TraceTreeBuilder(ITreeElement<?> root, TraceHelper traceHelper) {
        super(root);
        this.traceHelper = traceHelper;
    }

    @Override
    protected String getUrl(ITreeElement<?> element) {
        return FacesUtils.getContextPath() + "/faces/pages/modules/trace/showTraceTable.xhtml?"
            + Constants.REQUEST_PARAM_ID + "=" + traceHelper.getNodeKey(element);
    }

    @Override
    protected int getState(ITreeElement<?> element) {
        if (element instanceof DTConditionTraceObject) {
            return ((DTConditionTraceObject) element).isSuccessful() ? SUCCESSFUL : UNSUCCESSFUL;
        }

        return super.getState(element);
    }

}
