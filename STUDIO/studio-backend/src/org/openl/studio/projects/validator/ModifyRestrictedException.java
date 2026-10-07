package org.openl.studio.projects.validator;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.studio.common.exception.RestRuntimeException;

/**
 * Refuses a change to a project the current user cannot modify now, and says why.
 *
 * <p>The message names the project, and the user who holds its lock or the protected branch it is on. The refusal
 * answers {@code 409 Conflict}.
 *
 * @author Yury Molchan
 */
@Getter
@ResponseStatus(code = HttpStatus.CONFLICT)
public class ModifyRestrictedException extends RestRuntimeException {

    /** Why the project cannot be modified now. */
    private final ModifyRestriction restriction;

    public ModifyRestrictedException(UserWorkspaceProject project, ModifyRestriction restriction) {
        super(code(restriction), new Object[] {project.getBusinessName(), detail(project, restriction)});
        this.restriction = restriction;
    }

    private static String code(ModifyRestriction restriction) {
        return switch (restriction) {
            case LOCKED -> "file.project.locked.message";
            case BRANCH_PROTECTED -> "file.project.branch.protected.message";
        };
    }

    private static String detail(UserWorkspaceProject project, ModifyRestriction restriction) {
        return switch (restriction) {
            case LOCKED -> project.getLockInfo().getLockedBy();
            case BRANCH_PROTECTED -> project.getBranch();
        };
    }
}
