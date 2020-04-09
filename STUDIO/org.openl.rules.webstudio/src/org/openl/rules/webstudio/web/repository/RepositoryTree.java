package org.openl.rules.webstudio.web.repository;

import javax.faces.context.FacesContext;

import org.openl.rules.webstudio.web.repository.tree.TreeRepository;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Needed to render repository tree and to show error messages if failed to render it.
 */
@Controller
@RequestScope
public class RepositoryTree {
    private final RepositoryTreeState repositoryTreeState;

    public RepositoryTree(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;

        // Build tree if needed.
        repositoryTreeState.getRoot();

        // If during tree building found an error, show it.
        String errorMessage = repositoryTreeState.getErrorMessage();
        if (errorMessage != null) {
            WebStudioUtils.addErrorMessage(errorMessage);
        }
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
