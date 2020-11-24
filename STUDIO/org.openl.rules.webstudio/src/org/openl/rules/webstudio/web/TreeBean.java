package org.openl.rules.webstudio.web;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.RUN;

import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.CollectionUtils;
import org.richfaces.model.TreeNode;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Request scope managed bean providing logic for tree page of OpenL Studio.
 */
@Service
@SessionScope
public class TreeBean {

    private boolean hideUtilityTables = true;

    public void setHideUtilityTables(boolean hideUtilityTables) {
        this.hideUtilityTables = hideUtilityTables;
    }

    public boolean isHideUtilityTables() {
        return hideUtilityTables;
    }

    public void setCurrentView(String currentView) {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.setTreeView(currentView);
    }

    public boolean getCanRun() {
        return isGranted(RUN);
    }

    public int getProjectTestsCount() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        TestSuiteMethod[] allTestMethods = studio.getModel().getAllTestMethods();
        return CollectionUtils.isNotEmpty(allTestMethods) ? allTestMethods.length : 0;
    }

    public TreeNode getTree() {
        WebStudio studio = WebStudioUtils.getWebStudio();

        return studio.getModel().getProjectTree();
    }
}
