package org.openl.rules.ruleservice.core;


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
    private String projectName;
    private String moduleName;

    /**
     * Main constructor.
     * 
     * @param projectName project name
     * @param moduleName module name
     */
    ModuleDescription(String projectName, String moduleName) {
        if (projectName == null) {
            throw new IllegalArgumentException("projectName can't be null");
        }
        if (moduleName == null) {
            throw new IllegalArgumentException("moduleName can't be null");
        }

        this.projectName = projectName;
        this.moduleName = moduleName;
    }

    /**
     * Constructor for builder.
     * 
     * @param projectName project name
     * @param moduleName module name
     */
    private ModuleDescription(ModuleDescriptionBuilder builder) {
        this(builder.projectName, builder.moduleName);
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
        private String projectName;
        private String moduleName;

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
            if (projectName == null) {
                throw new IllegalStateException("projectName is required field for building ModuleDescription");
            }
            if (moduleName == null) {
                throw new IllegalStateException("moduleName is required field for building ModuleDescription");
            }

            return new ModuleDescription(this);
        }
    }
}