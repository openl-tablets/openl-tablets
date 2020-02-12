package org.openl.rules.webstudio.web.repository.merge;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.MergeConflictException;

public class MergeConflictInfo {
    private final MergeConflictException exception;
    private final RulesProject project;
    private final String mergeBranchFrom;
    private final String mergeBranchTo;

    public MergeConflictInfo(MergeConflictException exception, RulesProject project) {
        this.exception = exception;
        this.project = project;
        this.mergeBranchFrom = null;
        this.mergeBranchTo = null;
    }

    public MergeConflictInfo(MergeConflictException exception, RulesProject project, String mergeBranchFrom, String mergeBranchTo) {
        this.exception = exception;
        this.project = project;
        this.mergeBranchFrom = mergeBranchFrom;
        this.mergeBranchTo = mergeBranchTo;
    }

    public MergeConflictException getException() {
        return exception;
    }

    public RulesProject getProject() {
        return project;
    }

    public String getMergeBranchFrom() {
        return mergeBranchFrom;
    }

    public String getMergeBranchTo() {
        return mergeBranchTo;
    }

    public boolean isMerging() {
        return mergeBranchFrom != null && mergeBranchTo != null;
    }
}
