package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.io.Serial;
import java.util.Objects;

import lombok.Getter;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.Repository;

/**
 * A repository could not store what a project wrote into it: a database or a remote that cannot be reached, a folder
 * that cannot be written.
 *
 * <p>The failure of the repository is the cause, so a merge conflict a Git repository answers stays one.
 *
 * @author Yury Molchan
 */
@Getter
public class RepositoryWriteException extends ProjectException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The name of the repository, or its id when it has no name. */
    private final String repositoryName;

    public RepositoryWriteException(Repository repository, IOException cause) {
        super(cause.getMessage(), cause);
        this.repositoryName = Objects.requireNonNullElse(repository.getName(), repository.getId());
    }
}
