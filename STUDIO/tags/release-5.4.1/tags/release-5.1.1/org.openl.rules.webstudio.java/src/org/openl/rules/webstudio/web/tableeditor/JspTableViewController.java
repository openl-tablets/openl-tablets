package org.openl.rules.webstudio.web.tableeditor;

import javax.servlet.http.HttpServletRequest;

/**
 * This class allows using <code>BaseTableViewController</code> functionality from within a jsp page.
 * It is intended that the class is used in only request or page scope.
 *
 * @author Aliaksandr Antonik.
 */
public class JspTableViewController extends BaseTableViewController {
    public JspTableViewController(HttpServletRequest request) {
        super(new JSPTableEditorEnvironment(request));
    }
}
