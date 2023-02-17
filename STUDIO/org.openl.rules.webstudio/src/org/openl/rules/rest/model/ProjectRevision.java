package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

public class ProjectRevision extends HistoryLogModel {

    @Parameter(description = "If project was deleted or not.", required = true)
    private boolean deleted;

    @Parameter(description = "Comment parts. Always has 3 parts if present.")
    private List<String> commentParts;

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public List<String> getCommentParts() {
        return commentParts;
    }

    public void setCommentParts(List<String> commentParts) {
        this.commentParts = commentParts;
    }
}
