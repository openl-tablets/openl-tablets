package org.openl.rules.dtr.impl;

import org.openl.rules.commons.projects.VersionInfo;

import java.util.Date;

public class RepositoryVersionInfoImpl implements VersionInfo {
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
