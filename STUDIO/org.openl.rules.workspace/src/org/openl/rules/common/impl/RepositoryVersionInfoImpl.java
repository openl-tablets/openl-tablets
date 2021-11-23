package org.openl.rules.common.impl;

import java.util.Date;

import org.openl.rules.common.VersionInfo;

public class RepositoryVersionInfoImpl implements VersionInfo {
    private static final long serialVersionUID = 5338481693656986251L;

    private final Date createdAt;
    private final String createdBy;
    private final String emailCreatedBy;

    public RepositoryVersionInfoImpl(Date createdAt, String createdBy, String emailCreatedBy) {
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.emailCreatedBy = emailCreatedBy;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public String getEmailCreatedBy() {
        return emailCreatedBy;
    }

}
