package org.openl.rules.webstudio.web.admin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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

    public final String factoryId;

    @JsonValue
    public String getFactoryId() {
        return factoryId;
    }

    RepositoryType(String factoryId) {
        this.factoryId = factoryId;
    }
}
