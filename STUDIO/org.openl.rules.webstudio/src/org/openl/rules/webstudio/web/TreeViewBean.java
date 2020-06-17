package org.openl.rules.webstudio.web;

import javax.servlet.ServletRequest;

import org.openl.rules.webstudio.util.WebTool;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Request scope managed bean providing logic for TreeView page.
 */
@Service
@RequestScope
public class TreeViewBean {

    private static final String[] usedParams = { "title", "treePage", "mainPage", "mainFrame", "relWidth" };

    public String getTreePage() {
        return getPageUrl("treePage");
    }

    public String getMainPage() {
        return getPageUrl("mainPage");
    }

    public String getPagesWidth() {
        String mainPageWidth = WebStudioUtils.getRequestParameter("relWidth");
        if (StringUtils.isBlank(mainPageWidth)) {
            mainPageWidth = "65";
        }
        return String.format("*,%s%%", mainPageWidth);
    }

    public String getPageUrl(String pageName) {
        String treePage = WebStudioUtils.getRequestParameter(pageName);
        treePage += "?";
        treePage += WebTool.listRequestParams((ServletRequest) WebStudioUtils.getExternalContext().getRequest(), usedParams);
        return treePage;
    }

}
