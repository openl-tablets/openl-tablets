package org.openl.rules.webstudio.web;

import org.apache.commons.lang.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.commons.web.util.WebTool;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringTool;

/**
 * Request scope managed bean providing logic for openTable page.
 */
public class OpenTableBean {

    private final String contextPath = FacesUtils.getContextPath();

    private final String TREE_PAGE = contextPath + "/faces/facelets/leftPanel.xhtml";
    private final String TABLE_PAGE = contextPath + "/faces/facelets/tableeditor/showTable.xhtml";
    private final String INTRO_PAGE = contextPath + "/home.html";
    private final String FOOTER_PAGE = contextPath + "/faces/facelets/footerPanel.xhtml";

    private String leftPage;
    private String mainPage;

    public OpenTableBean() {
        leftPage = TREE_PAGE;
        mainPage = INTRO_PAGE;

        WebStudio studio = WebStudioUtils.getWebStudio();
        String reload = FacesUtils.getRequestParameter("reload");
        if ("true".equals(reload)) {
            studio.rebuildModel();
        }

        String tableUri = null;
        String reopenCurrentTable = FacesUtils.getRequestParameter("reopen");
        if ("true".equals(reopenCurrentTable)) {
            tableUri = studio.getTableUri();
        } else {
            tableUri = FacesUtils.getRequestParameter("uri");
        }

        if (StringUtils.isNotBlank(tableUri)) {
            String nodeToOpen = studio.getModel().getTreeNodeId(tableUri);
            if (StringUtils.isNotBlank(nodeToOpen)) {
                leftPage += "?nodeToOpen=" + nodeToOpen;
            }
            String tableParams = WebTool.listRequestParams(FacesUtils.getRequest(), new String[]{ "uri" });
            tableParams += ((StringUtils.isEmpty(tableParams) ? "" : "&") + "uri=" + StringTool.encodeURL(tableUri));
            mainPage = TABLE_PAGE + "?" + tableParams;
        }
    }

    public String getLeftPage() {
        return leftPage;
    }

    public String getMainPage() {
        return mainPage;
    }

    public String getFooterPage() {
        return FOOTER_PAGE;
    }

}
