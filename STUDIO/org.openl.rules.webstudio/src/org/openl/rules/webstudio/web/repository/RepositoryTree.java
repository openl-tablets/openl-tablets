package org.openl.rules.webstudio.web.repository;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.webstudio.web.repository.tree.TreeRepository;

/**
 * Needed to render repository tree and to show error messages if failed to render it.
 */
@ManagedBean
@RequestScoped
public class RepositoryTree {
    @ManagedProperty(value = "#{repositoryTreeState}")
    private RepositoryTreeState repositoryTreeState;

    private TreeRepository root;

    @PostConstruct
    public void afterPropertiesSet() {
        // Build tree if needed.
        root = repositoryTreeState.getRoot();

        // If during tree building found an error, show it.
        String errorMessage = repositoryTreeState.getErrorMessage();
        if (errorMessage != null) {
            FacesUtils.addErrorMessage(errorMessage);
        }
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public TreeRepository getRoot() {
        return root;
    }

    public boolean isHasMessages() {
        return FacesUtils.getFacesContext().getMaximumSeverity() != null && FacesUtils.getFacesContext()
            .getMessages(null)
            .hasNext();
    }
}
