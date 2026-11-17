package org.openl.rules.webstudio.web.repository;

import java.io.Serial;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * A project of a deployment that cannot be read from the design repository: it is not found there, it has no files,
 * or a file cannot be read. The deployment stops rather than leave the project out.
 *
 * @author Yury Molchan
 */
@Getter
public class UnreadableProjectException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String projectName;

    public UnreadableProjectException(String projectName, @Nullable Throwable cause) {
        super("The project '%s' cannot be read from the design repository.".formatted(projectName), cause);
        this.projectName = projectName;
    }
}
