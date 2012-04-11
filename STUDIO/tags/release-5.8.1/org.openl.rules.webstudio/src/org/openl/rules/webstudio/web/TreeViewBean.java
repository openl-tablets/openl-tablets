package org.openl.rules.webstudio.web;

import org.apache.commons.lang.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.commons.web.util.WebTool;

/**
 * Request scope managed bean providing logic for TreeView page.
 */
public class TreeViewBean {

    private static final String[] usedParams={"title", "treePage", "mainPage", "mainFrame", "relWidth"};

    public TreeViewBean() {
    }

    public String getTreePage() {
        return getPageUrl("treePage");
    }

    public String getMainPage() {
        return getPageUrl("mainPage");
    }

    public String getPagesWidth() {
        String mainPageWidth = FacesUtils.getRequestParameter("relWidth");
        if (StringUtils.isBlank(mainPageWidth)) {
            mainPageWidth = "70";
        }
        return String.format("*,%s%%", mainPageWidth);
    }

    public String getPageUrl(String pageName) {
        String treePage = FacesUtils.getRequestParameter(pageName);
        treePage += "?";
        treePage += WebTool.listRequestParams(FacesUtils.getRequest(), usedParams);
        return treePage;
    }

}
