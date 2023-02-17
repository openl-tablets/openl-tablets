package org.openl.rules.rest.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.v3.oas.annotations.Parameter;

public class HistoryLogModel {

    @Parameter(description = "Revision number", required = true)
    private String revisionNo;

    @Parameter(description = "Short revision number")
    private String shortRevisionNo;

    @Parameter(description = "Creation date-time", required = true)
    private Date createdAt;

    @Parameter(description = "Full comment", required = true)
    private String fullComment;

    @Parameter(description = "Author")
    @JsonView({ UserInfoModel.View.Short.class })
    private UserInfoModel author;

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
}
