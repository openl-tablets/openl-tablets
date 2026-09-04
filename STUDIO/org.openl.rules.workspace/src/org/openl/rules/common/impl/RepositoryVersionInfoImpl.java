package org.openl.rules.common.impl;

import java.io.Serial;
import java.util.Date;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.rules.common.VersionInfo;

@RequiredArgsConstructor
public class RepositoryVersionInfoImpl implements VersionInfo {
    @Serial
    private static final long serialVersionUID = 5338481693656986251L;

    @Getter
    private final Date createdAt;
    @Getter
    private final String createdBy;
    @Getter
    private final String emailCreatedBy;

}
