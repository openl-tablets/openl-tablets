package org.openl.rules.webstudio.web.admin;

public enum RepositoryType {
    DB(org.openl.rules.repository.db.JdbcDBRepositoryFactory.class),
    JNDI(org.openl.rules.repository.db.DatasourceDBRepositoryFactory.class),
    AWS_S3(org.openl.rules.repository.aws.S3Repository.class),
    GIT(org.openl.rules.repository.git.GitRepository.class);

    public static RepositoryType findByAccessType(String accessType) {
        for (RepositoryType repositoryType : values()) {
            if (repositoryType.accessType.equals(accessType)) {
                return repositoryType;
            }
        }

        return null;
    }

    public static RepositoryType findByFactory(String className) {
        for (RepositoryType repositoryType : values()) {
            if (repositoryType.factoryClassName.equals(className)) {
                return repositoryType;
            }
        }

        return null;
    }

    private final String accessType;
    private final String factoryClassName;

    RepositoryType(Class<?> factoryClass) {
        this.factoryClassName = factoryClass.getName();
        this.accessType = name().toLowerCase();
    }

    public String getFactoryClassName() {
        return factoryClassName;
    }
}
