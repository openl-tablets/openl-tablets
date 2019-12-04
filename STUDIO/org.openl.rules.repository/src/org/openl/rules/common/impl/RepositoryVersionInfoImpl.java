package org.openl.rules.common.impl;

import java.util.Date;

import org.openl.rules.common.VersionInfo;

public class RepositoryVersionInfoImpl implements VersionInfo {
    private static final long serialVersionUID = 5338481693656986251L;

    private Date createdAt;
    private String createdBy;

    public RepositoryVersionInfoImpl(Date createdAt, String createdBy) {
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

}
