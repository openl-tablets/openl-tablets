package org.openl.rules.webstudio.web.repository;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

import org.openl.rules.webstudio.web.repository.tree.TreeRepository;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * Needed to render repository tree and to show error messages if failed to render it.
 */
@ManagedBean
@RequestScoped
public class RepositoryTree {
    @ManagedProperty(value = "#{repositoryTreeState}")
    private RepositoryTreeState repositoryTreeState;

    @PostConstruct
    public void afterPropertiesSet() {
        // Build tree if needed.
        repositoryTreeState.getRoot();

        // If during tree building found an error, show it.
        String errorMessage = repositoryTreeState.getErrorMessage();
        if (errorMessage != null) {
            WebStudioUtils.addErrorMessage(errorMessage);
        }
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public TreeRepository getRoot() {
        return repositoryTreeState.getRoot();
    }

    public boolean isHasMessages() {
        return FacesContext.getCurrentInstance().getMaximumSeverity() != null && FacesContext.getCurrentInstance()
            .getMessages(null)
            .hasNext();
    }
}
