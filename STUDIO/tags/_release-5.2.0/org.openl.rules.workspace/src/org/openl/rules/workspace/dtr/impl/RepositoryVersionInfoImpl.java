package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.workspace.abstracts.VersionInfo;

import java.util.Date;

public class RepositoryVersionInfoImpl implements VersionInfo {
    private static final long serialVersionUID = 5338481693656986251L;

    private Date createdAt;
    private String createdBy;

    public RepositoryVersionInfoImpl(Date createdAt, String createdBy) {
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
