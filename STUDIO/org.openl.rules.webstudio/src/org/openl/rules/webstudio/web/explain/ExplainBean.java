package org.openl.rules.webstudio.web.explain;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.commons.web.jsf.FacesUtils;
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

        List<String[]> result = Explanator.getExplainList(rootID, expandID);
        return result;
    }
}
