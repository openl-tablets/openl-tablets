package org.openl.rules.webstudio.web.repository.merge;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.MergeConflictException;

public class MergeConflictInfo {
    private final MergeConflictException exception;
    private final RulesProject project;

    public MergeConflictInfo(MergeConflictException exception, RulesProject project) {
        this.exception = exception;
        this.project = project;
    }

    public MergeConflictException getException() {
        return exception;
    }

    public RulesProject getProject() {
        return project;
    }
}
