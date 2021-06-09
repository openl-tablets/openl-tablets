package org.openl.rules.webstudio.web.admin;

public enum RepositoryType {
    DB("repo-jdbc"),
    JNDI("repo-jndi"),
    AWS_S3("repo-aws-s3"),
    GIT("repo-git"),
    LOCAL("repo-file");

    public static RepositoryType findByFactory(String id) {
        for (RepositoryType repositoryType : values()) {
            if (repositoryType.factoryId.equals(id)) {
                return repositoryType;
            }
        }

        return null;
    }

    public final String factoryId;

    RepositoryType(String factoryId) {
        this.factoryId = factoryId;
    }
}
