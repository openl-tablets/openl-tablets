package org.openl.rules.webstudio.web.explain;

import java.util.ArrayList;
import java.util.List;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.ui.Explanation;
import org.openl.rules.ui.Explanator;
import org.openl.rules.webstudio.web.util.Constants;

/**
 * Request scope managed bean for Explain page.
 */
public class ExplainBean {

    private boolean showNames;
    private boolean showValues;
    private Explanator explanator;
    private Explanation explanation;

    public ExplainBean() {
        String showNamesStr = FacesUtils.getRequestParameter("showNames");
        showNames = "true".equals(showNamesStr);

        String showValuesStr = FacesUtils.getRequestParameter("showValues");
        showValues = "true".equals(showValuesStr);

        explanator = (Explanator) FacesUtils.getSessionParam(Constants.SESSION_PARAM_EXPLANATOR);
        String rootID = FacesUtils.getRequestParameter("rootID");
        explanation = explanator.getExplanation(rootID);
    }

    public String[] getExplainTree() {
        String header = FacesUtils.getRequestParameter("header");
        String expandID = FacesUtils.getRequestParameter("expandID");

        explanation.setShowNamesInFormula(showNames);
        explanation.setShowValuesInFormula(showValues);
        explanation.setHeader(header);
        if (expandID != null) {
             explanation.expand(expandID);
        }

        return explanation.htmlTable(explanation.getExplainTree());
    }

    public List<String[]> getExpandedValues() {
        List<String[]> expandedValuesList = new ArrayList<String[]>();

        List<ExplanationNumberValue<?>> expandedValues = explanation.getExpandedValues();
        for (ExplanationNumberValue<?> explanationValue : expandedValues) {
            String[] html = explanation.htmlTable(explanationValue);
            expandedValuesList.add(html);
        }

        return expandedValuesList;
    }

    public boolean isShowNames() {
        return showNames;
    }
    
    public void setShowNames(boolean showNames) {
        this.showNames = showNames;
    }

    public boolean isShowValues() {
        return showValues;
    }

    public void setShowValues(boolean showValues) {
        this.showValues = showValues;
    }

}
