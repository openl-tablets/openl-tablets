package org.openl.rules.ui.tree.richfaces;

import org.apache.commons.lang.StringUtils;
import org.openl.base.INamedThing;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.util.StringTool;
import org.openl.util.tree.ITreeElement;

public class ExplainTreeBuilder extends TreeBuilder {

    private static final String showTablePage = "/faces/facelets/explain/showExplainTable.xhtml?";

    public ExplainTreeBuilder(ITreeElement<?> root) {
        super(root);
    }

    @Override
    protected String getDisplayName(Object obj, int mode) {
        return super.getDisplayName(obj, mode + 1);
    }

    @Override
    protected String getUrl(ITreeElement<?> element) {
        ExplanationNumberValue<?> explanationValue = (ExplanationNumberValue<?>) element;
        String url = explanationValue.getMetaInfo() == null ? null : explanationValue.getMetaInfo().getSourceUrl();
        if (StringUtils.isNotBlank(url)) {
            return getUrlToElement(element, url);
        }
        return FacesUtils.getContextPath() + showTablePage;
    }

    private String getUrlToElement(ITreeElement<?> element, String url) {
        return FacesUtils.getContextPath() + showTablePage
        + Constants.REQUEST_PARAM_URI + "=" + StringTool.encodeURL("" + url)
        + "&text=" + getDisplayName(element, INamedThing.REGULAR);
    }

}
