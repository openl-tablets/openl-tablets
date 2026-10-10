package org.openl.rules.project.abstraction;

import java.io.Serial;
import java.util.List;

import lombok.Getter;

import org.openl.rules.common.ProjectException;

/**
 * A save refused because another program changed, in the design repository, files the save would write or delete.
 *
 * <p>Saving would overwrite what that program wrote, so nothing is saved.
 *
 * @author Yury Molchan
 */
@Getter
public class ChangedOutsideException extends ProjectException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The files changed on both sides, by their paths in the project, in name order. */
    private final transient List<String> paths;

    public ChangedOutsideException(List<String> paths) {
        super("Files changed outside OpenL Studio as well: " + String.join(", ", paths));
        this.paths = List.copyOf(paths);
    }
}
