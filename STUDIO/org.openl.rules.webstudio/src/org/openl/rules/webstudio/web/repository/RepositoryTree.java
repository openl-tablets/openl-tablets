package org.openl.rules.webstudio.web.repository;

import javax.faces.context.FacesContext;

import org.openl.rules.webstudio.web.repository.tree.TreeRepository;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;

/**
 * Needed to render repository tree and to show error messages if failed to render it.
 */
@Service
@RequestScope
public class RepositoryTree {
    private final RepositoryTreeState repositoryTreeState;

    public RepositoryTree(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;

        // Build tree if needed.
        repositoryTreeState.getRoot();
    }

    public TreeRepository getRoot() {
        return repositoryTreeState.getRoot();
    }

    public boolean isHasMessages() {
        boolean hasMessages = FacesContext.getCurrentInstance().getMaximumSeverity() != null && FacesContext.getCurrentInstance()
                .getMessages(null)
                .hasNext();
        if (hasMessages) {
            return true;
        }

        // Error can be absent at the beginning of current request, but added before end of request.
        List<String> errorMessages = repositoryTreeState.getErrorsContainer().getErrors();
        if (!errorMessages.isEmpty()) {
            for (String errorMessage : errorMessages) {
                WebStudioUtils.addErrorMessage(errorMessage);
            }
            return true;
        }

        return false;
    }
}
