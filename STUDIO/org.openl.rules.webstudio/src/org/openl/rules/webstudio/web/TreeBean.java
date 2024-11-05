package org.openl.rules.webstudio.web;

import java.util.List;

import org.richfaces.model.TreeNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclService;

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

    private final RepositoryAclService designRepositoryAclService;

    public TreeBean(@Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService) {
        this.designRepositoryAclService = designRepositoryAclService;
    }

    public void setCurrentView(String currentView) {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.setTreeView(currentView);
    }

    public boolean getCanRun() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        return designRepositoryAclService.isGranted(studio.getCurrentProject(), List.of(AclPermission.VIEW));
    }

    public TreeNode getTree() {
        WebStudio studio = WebStudioUtils.getWebStudio();

        return studio.getModel().getProjectTree();
    }
}
