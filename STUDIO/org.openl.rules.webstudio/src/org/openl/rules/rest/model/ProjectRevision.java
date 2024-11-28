package org.openl.rules.rest.model;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;

public class ProjectRevision {

    @Parameter(description = "Revision number", required = true)
    private String revisionNo;

    @Parameter(description = "Short revision number")
    private String shortRevisionNo;

    @Parameter(description = "Creation date-time", required = true)
    private Date createdAt;

    @Parameter(description = "Full comment", required = true)
    private String fullComment;

    @Parameter(description = "Author")
    @JsonView({UserInfoModel.View.Short.class})
    private UserInfoModel author;

    @Parameter(description = "If project was deleted or not.", required = true)
    private boolean deleted;

    @Parameter(description = "If current revision has changes in the project or not.", required = true)
    private boolean technicalRevision;

    @Parameter(description = "Comment parts. Always has 3 parts if present.")
    private List<String> commentParts;

    public String getRevisionNo() {
        return revisionNo;
    }

    public void setRevisionNo(String revisionNo) {
        this.revisionNo = revisionNo;
    }

    public String getShortRevisionNo() {
        return shortRevisionNo;
    }

    public void setShortRevisionNo(String shortRevisionNo) {
        this.shortRevisionNo = shortRevisionNo;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getFullComment() {
        return fullComment;
    }

    public void setFullComment(String fullComment) {
        this.fullComment = fullComment;
    }

    public UserInfoModel getAuthor() {
        return author;
    }

    public void setAuthor(UserInfoModel author) {
        this.author = author;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isTechnicalRevision() {
        return technicalRevision;
    }

    public void setTechnicalRevision(boolean technicalRevision) {
        this.technicalRevision = technicalRevision;
    }

    public List<String> getCommentParts() {
        return commentParts;
    }

    public void setCommentParts(List<String> commentParts) {
        this.commentParts = commentParts;
    }
}
