package org.openl.rules.webstudio.web.tableeditor;

import javax.servlet.http.HttpServletRequest;

import org.openl.rules.ui.WebStudio;

/**
 * This class allows using <code>BaseTableViewController</code> functionality from within a jsp page.
 * It is intended that the class is used in only request or page scope.
 *
 * @author Aliaksandr Antonik.
 */
public class JspTableViewController extends BaseTableViewController {
    private HttpServletRequest request;

    public JspTableViewController(HttpServletRequest request) {
        this.request = request;
    }

    protected WebStudio getWebstudio() {
        return (WebStudio) request.getSession().getAttribute("studio");
    }

    protected int getElementId() {
        try {
            return Integer.parseInt(request.getParameter("elementID"));
        } catch (NumberFormatException e) {
            return getWebstudio().getTableID();
        }
    }
}
