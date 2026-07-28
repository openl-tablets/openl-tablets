package org.openl.rules.common.impl;

import java.io.Serial;
import java.util.Date;

import lombok.RequiredArgsConstructor;

import org.openl.rules.common.VersionInfo;

@RequiredArgsConstructor
public class RepositoryVersionInfoImpl implements VersionInfo {
    @Serial
    private static final long serialVersionUID = 5338481693656986251L;

    private final Date createdAt;
    private final String createdBy;
    private final String emailCreatedBy;

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
