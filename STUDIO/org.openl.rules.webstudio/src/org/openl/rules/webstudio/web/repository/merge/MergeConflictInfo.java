package org.openl.rules.webstudio.web.repository.merge;

import java.util.Objects;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.MergeConflictException;

public class MergeConflictInfo {
    private final MergeConflictException exception;
    private final RulesProject project;
    private final String mergeBranchFrom;
    private final String mergeBranchTo;
    private final String currentBranch;

    public MergeConflictInfo(MergeConflictException exception, RulesProject project) {
        this.exception = exception;
        this.project = project;
        this.mergeBranchFrom = null;
        this.mergeBranchTo = null;
        this.currentBranch = null;
    }

    public MergeConflictInfo(MergeConflictException exception,
        RulesProject project,
        String mergeBranchFrom,
        String mergeBranchTo,
        String currentBranch) {
        Objects.requireNonNull(mergeBranchFrom, "mergeBranchFrom must be initialized");
        Objects.requireNonNull(mergeBranchTo, "mergeBranchTo must be initialized");
        Objects.requireNonNull(currentBranch, "currentBranch must be initialized");
        this.exception = exception;
        this.project = project;
        this.mergeBranchFrom = mergeBranchFrom;
        this.mergeBranchTo = mergeBranchTo;
        this.currentBranch = currentBranch;
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

    public boolean isExportOperation() {
        return mergeBranchFrom != null && mergeBranchFrom.equals(currentBranch);
    }
}
