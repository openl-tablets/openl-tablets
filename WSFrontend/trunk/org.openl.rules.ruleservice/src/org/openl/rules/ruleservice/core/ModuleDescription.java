package org.openl.rules.ruleservice.core;

import org.openl.rules.common.CommonVersion;

/**
 * Class designed for storing module info. ModuleDescriptionBuilder can be used
 * for build ModuleDescription instances.
 * 
 * Immutable.
 * 
 * @author Marat Kamalov
 * 
 */
public final class ModuleDescription {
    private String deploymentName;
    private CommonVersion deploymentVersion;
    private String projectName;
    private String moduleName;

    /**
     * Main constructor.
     * 
     * @param deploymentName deployment name
     * @param deploymentVersion deployment version
     * @param projectName project name
     * @param moduleName module name
     */
    ModuleDescription(String deploymentName, CommonVersion deploymentVersion, String projectName, String moduleName) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName can't be null");
        }
        if (deploymentVersion == null) {
            throw new IllegalArgumentException("deploymentVersion can't be null");
        }
        if (projectName == null) {
            throw new IllegalArgumentException("projectName can't be null");
        }
        if (moduleName == null) {
            throw new IllegalArgumentException("moduleName can't be null");
        }

        this.deploymentName = deploymentName;
        this.deploymentVersion = deploymentVersion;
        this.projectName = projectName;
        this.moduleName = moduleName;
    }

    /**
     * Constructor for builder.
     * 
     * @param deploymentName deployment name
     * @param deploymentVersion deployment version
     * @param projectName project name
     * @param moduleName module name
     */
    private ModuleDescription(ModuleDescriptionBuilder builder) {
        this(builder.deploymentName, builder.deploymentVersion, builder.projectName, builder.moduleName);
    }

    /**
     * Returns a deployment name.
     * 
     * @return deployment name
     */
    public String getDeploymentName() {
        return deploymentName;
    }

    /**
     * Returns the deployment version.
     * 
     * @return deployment version
     */
    public CommonVersion getDeploymentVersion() {
        return deploymentVersion;
    }

    /**
     * Returns the project name.
     * 
     * @return project name
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Returns the module name.
     * 
     * @return module name
     */
    public String getModuleName() {
        return moduleName;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((deploymentName == null) ? 0 : deploymentName.hashCode());
        result = prime * result + ((deploymentVersion == null) ? 0 : deploymentVersion.hashCode());
        result = prime * result + ((moduleName == null) ? 0 : moduleName.hashCode());
        result = prime * result + ((projectName == null) ? 0 : projectName.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ModuleDescription other = (ModuleDescription) obj;
        if (deploymentName == null) {
            if (other.deploymentName != null) {
                return false;
            }
        } else if (!deploymentName.equals(other.deploymentName))
            return false;
        if (deploymentVersion == null) {
            if (other.deploymentVersion != null) {
                return false;
            }
        } else if (!deploymentVersion.equals(other.deploymentVersion)) {
            return false;
        }
        if (moduleName == null) {
            if (other.moduleName != null) {
                return false;
            }
        } else if (!moduleName.equals(other.moduleName)) {
            return false;
        }
        if (projectName == null) {
            if (other.projectName != null) {
                return false;
            }
        } else if (!projectName.equals(other.projectName)) {
            return false;
        }
        return true;
    }

    /**
     * Builder for ModuleDescription.
     * 
     * @author Marat Kamalov
     * 
     */
    public static final class ModuleDescriptionBuilder {
        private String deploymentName;
        private CommonVersion deploymentVersion;
        private String projectName;
        private String moduleName;

        /**
         * Sets deploymentName to the builder.
         * 
         * @param deploymentName deployment name
         * @return
         */
        public ModuleDescriptionBuilder setDeploymentName(String deploymentName) {
            if (deploymentName == null) {
                throw new IllegalArgumentException("deploymentName can't be null");
            }

            this.deploymentName = deploymentName;
            return this;
        }

        /**
         * Sets deployment version to the builder.
         * 
         * @param deploymentVersion deployment version
         * @return
         */
        public ModuleDescriptionBuilder setDeploymentVersion(CommonVersion deploymentVersion) {
            if (deploymentVersion == null) {
                throw new IllegalArgumentException("deploymentVersion can't be null");
            }

            this.deploymentVersion = deploymentVersion;
            return this;
        }

        /**
         * Sets project name to the builder.
         * 
         * @param projectName project name
         * @return
         */
        public ModuleDescriptionBuilder setProjectName(String projectName) {
            if (projectName == null) {
                throw new IllegalArgumentException("projectName can't be null");
            }

            this.projectName = projectName;
            return this;
        }

        /**
         * Set module name to the the builder.
         * 
         * @param moduleName module name
         * @return
         */
        public ModuleDescriptionBuilder setModuleName(String moduleName) {
            if (moduleName == null) {
                throw new IllegalArgumentException("moduleName can't be null");
            }

            this.moduleName = moduleName;
            return this;
        }

        /**
         * Builds ModuleDesctiption.
         * 
         * @return
         */
        public ModuleDescription build() {
            if (deploymentName == null) {
                throw new IllegalStateException("deploymentName is required field for building ServiceDescription");
            }
            if (deploymentVersion == null) {
                throw new IllegalStateException("deploymentVersion is required field for building ServiceDescription");
            }
            if (projectName == null) {
                throw new IllegalStateException("projectName is required field for building ServiceDescription");
            }
            if (moduleName == null) {
                throw new IllegalStateException("moduleName is required field for building ServiceDescription");
            }

            return new ModuleDescription(this);
        }
    }
}