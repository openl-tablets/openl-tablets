package org.openl.rules.webstudio.web.repository;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ManagedBean
@ViewScoped
public class BranchesBean {
    private final Logger log = LoggerFactory.getLogger(BranchesBean.class);

    private String currentProjectName;

    private List<String> branches;

    public String getCurrentProjectName() {
        return currentProjectName;
    }

    public void setCurrentProjectName(String currentProjectName) {
        this.currentProjectName = currentProjectName;
    }

    public List<String> getBranches() {
        return branches;
    }

    public void setBranches(List<String> branches) {
        this.branches = branches;
    }

    public List<String> getAvailableBranches() {
        RulesProject project = getProject(currentProjectName);
        if (project != null) {
            Repository repository = project.getDesignRepository();
            if (repository.supports().branches()) {
                try {
                    return ((BranchRepository) repository).getBranches(null);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return Collections.emptyList();
    }

    public void save() {
        try {
            RulesProject project = getProject(currentProjectName);
            if (project != null) {
                BranchRepository repository = (BranchRepository) project.getDesignRepository();

                List<String> existingBranches = getBranches(project);
                for (String branch : branches) {
                    if (!existingBranches.contains(branch)) {
                        repository.createBranch(currentProjectName, branch);
                    }
                }

                for (String branch : existingBranches) {
                    if (!branches.contains(branch)) {
                        repository.deleteBranch(currentProjectName, branch);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            FacesUtils.addErrorMessage("Can't copy the project: " + e.getMessage());
        }
    }

    public void setInitProject(String currentProjectName) {
        try {
            this.currentProjectName = currentProjectName;

            RulesProject project = getProject(currentProjectName);
            if (project != null) {
                branches = getBranches(project);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private List<String> getBranches(RulesProject project) {
        String projectName = project.getName();
        Repository repository = project.getDesignRepository();
        if (repository.supports().branches()) {
            try {
                return ((BranchRepository) repository).getBranches(projectName);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

        return Collections.emptyList();
    }

    private RulesProject getProject(String projectName) {
        if (StringUtils.isBlank(projectName)) {
            return null;
        }

        try {
            UserWorkspace userWorkspace = getUserWorkspace();
            if (!userWorkspace.hasProject(projectName)) {
                return null;
            }

            return userWorkspace.getProject(projectName, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private UserWorkspace getUserWorkspace() throws WorkspaceException {
        RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession(FacesUtils.getSession());
        return rulesUserSession.getUserWorkspace();
    }
}
