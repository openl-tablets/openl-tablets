package org.openl.rules.webstudio.web.admin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum RepositoryType {
    DB("repo-jdbc"),
    JNDI("repo-jndi"),
    AWS_S3("repo-aws-s3"),
    AZURE("repo-azure-blob"),
    GIT("repo-git"),
    LOCAL("repo-file");

    @JsonCreator
    public static RepositoryType findByFactory(String id) {
        for (RepositoryType repositoryType : values()) {
            if (repositoryType.factoryId.equals(id)) {
                return repositoryType;
            }
        }

        return null;
    }

    @Getter(onMethod_ = {@JsonValue})
    public final String factoryId;
}
