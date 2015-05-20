package org.openl.rules.webstudio.web.explain;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.ui.Explanation;
import org.openl.rules.ui.Explanator;

/**
 * Request scope managed bean for Explain page.
 */
@ManagedBean
@RequestScoped
public class ExplainBean {

    public List<String[]> getExpandedValues() {
        String rootID = FacesUtils.getRequestParameter("rootID");
        String expandID = FacesUtils.getRequestParameter("expandID");
        String fromID = FacesUtils.getRequestParameter("from");

        Explanation explanation = Explanator.getRootExplanation(rootID);
        if (expandID != null) {
            explanation.expand(expandID, fromID);
        }
        List<String[]> expandedValuesList = new ArrayList<String[]>();

        List<ExplanationNumberValue<?>> expandedValues = explanation.getExpandedValues();
        for (ExplanationNumberValue<?> explanationValue : expandedValues) {
            String[] html = explanation.htmlTable(explanationValue);
            expandedValuesList.add(html);
        }

        return expandedValuesList;
    }

}
