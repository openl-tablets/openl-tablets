package org.openl.rules.webstudio.web.admin;

public enum JcrType {
    DESIGN_LOCAL(org.openl.rules.repository.factories.LocalJackrabbitDesignRepositoryFactory.class),
    DESIGN_RMI(org.openl.rules.repository.factories.RmiJackrabbitDesignRepositoryFactory.class),
    DESIGN_WEBDAV(org.openl.rules.repository.factories.WebDavJackrabbitDesignRepositoryFactory.class),
    DESIGN_DB(org.openl.rules.repository.factories.DBDesignRepositoryFactory.class),
    PRODUCTION_LOCAL(org.openl.rules.repository.factories.LocalJackrabbitProductionRepositoryFactory.class),
    PRODUCTION_RMI(org.openl.rules.repository.factories.RmiJackrabbitProductionRepositoryFactory.class),
    PRODUCTION_WEBDAV(org.openl.rules.repository.factories.WebDavJackrabbitProductionRepositoryFactory.class),
    PRODUCTION_DB(org.openl.rules.repository.factories.DBProductionRepositoryFactory.class);

    public static JcrType findByAccessType(RepositoryType repositoryType, String accessType) {
        for (JcrType jcrType : values()) {
            if (jcrType.repositoryType == repositoryType && jcrType.accessType.equals(accessType)) {
                return jcrType;
            }
        }

        return null;
    }

    public static JcrType findByFactory(RepositoryType repositoryType, String factoryClassName) {
        for (JcrType jcrType : values()) {
            if (jcrType.repositoryType == repositoryType && jcrType.factoryClassName.equals(factoryClassName)) {
                return jcrType;
            }
        }

        return null;
    }

    private final String accessType;
    private final RepositoryType repositoryType;
    private final String factoryClassName;

    private JcrType(Class factoryClass) {
        this.factoryClassName = factoryClass.getName();

        // Reduce parameters mess by parsing enum name: it contains all needed information
        String[] namePart = name().split("_");
        this.repositoryType = RepositoryType.valueOf(namePart[0]);
        this.accessType = namePart[1].toLowerCase();
    }

    public String getAccessType() {
        return accessType;
    }

    public boolean isLocal() {
        return "local".equals(accessType);
    }

    public String getFactoryClassName() {
        return factoryClassName;
    }
}
