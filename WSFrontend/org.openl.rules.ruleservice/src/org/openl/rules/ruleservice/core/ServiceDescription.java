package org.openl.rules.ruleservice.core;

import java.util.*;

import org.openl.rules.project.model.Module;

/**
 * Class designed for storing service info.
 *
 * Immutable.
 *
 * @author Marat Kamalov
 *
 */
public final class ServiceDescription {
    private String name;
    private String url;
    private String serviceClassName;
    private String rmiServiceClassName;
    private String rmiName;
    private String annotationTemplateClassName;
    private boolean provideRuntimeContext;
    private boolean provideVariations;
    private Map<String, Object> configuration;
    private Collection<Module> modules;
    private DeploymentDescription deployment;
    private String[] publishers;
    private ResourceLoader resourceLoader;

    /**
     * Main constructor.
     *
     * @param name
     * @param url
     * @param serviceClassName
     * @param provideRuntimeContext
     * @param provideVariations
     * @param modules
     */
    ServiceDescription(String name,
            String url,
            String serviceClassName,
            String rmiServiceClassName,
            String rmiName,
            String annotationTemplateClassName,
            boolean provideRuntimeContext,
            boolean provideVariations,
            Collection<Module> modules,
            DeploymentDescription deployment,
            Map<String, Object> configuration,
            String[] publishers,
            ResourceLoader resourceLoader) {
        Objects.requireNonNull(name, "name arg must not be null.");
        Objects.requireNonNull(resourceLoader, "resourceLoader arg must not be null.");
        this.name = name;
        this.url = url;
        this.serviceClassName = serviceClassName;
        this.provideRuntimeContext = provideRuntimeContext;
        this.rmiServiceClassName = rmiServiceClassName;
        this.rmiName = rmiName;
        this.provideVariations = provideVariations;
        this.annotationTemplateClassName = annotationTemplateClassName;
        if (configuration == null) {
            this.configuration = Collections.emptyMap();
        } else {
            this.configuration = Collections.unmodifiableMap(configuration);
        }
        if (modules != null) {
            this.modules = Collections.unmodifiableCollection(modules);
        } else {
            this.modules = Collections.emptySet();
        }

        this.publishers = publishers;
        this.deployment = deployment;
        this.resourceLoader = resourceLoader;
    }

    private ServiceDescription(ServiceDescriptionBuilder builder) {
        this(builder.name,
            builder.url,
            builder.serviceClassName,
            builder.rmiServiceClassName,
            builder.rmiName,
            builder.annotationTemplateClassName,
            builder.provideRuntimeContext,
            builder.provideVariations,
            builder.modules,
            builder.deployment,
            builder.configuration,
            builder.publishers.toArray(new String[] {}),
            builder.resourceLoader);
    }

    /**
     * Returns annotation template class name
     *
     * @return class name
     */
    public String getAnnotationTemplateClassName() {
        return annotationTemplateClassName;
    }

    /**
     * Returns service name.
     *
     * @return service name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns service URL.
     *
     * @return
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns service class name.
     *
     * @return class name
     */
    public String getServiceClassName() {
        return serviceClassName;
    }

    /**
     * Returns RMI service class name.
     *
     * @return class name
     */
    public String getRmiServiceClassName() {
        return rmiServiceClassName;
    }

    /**
     * Returns RMI name.
     *
     * @return RMI name
     */
    public String getRmiName() {
        return rmiName;
    }

    /**
     * Returns provideRuntimeContext value. This value is define that service methods first argument is
     * IRulesRuntimeContext.
     *
     * @return
     */
    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    /**
     * This flag defines whether variations will be supported or not.
     *
     * @return <code>true</code> if variations should be injected in service class, and <code>false</code> otherwise.
     */
    public boolean isProvideVariations() {
        return provideVariations;
    }

    /**
     * Returns modules for the deployment.
     *
     * @return a set of modules
     */
    public Collection<Module> getModules() {
        return modules;
    }
    
    /**
     * Returns resourceLoader for the deployment.
     *
     * @return resourceLoader
     */
    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    /**
     * Retuns configuration
     *
     * @return configuration
     */
    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public DeploymentDescription getDeployment() {
        return deployment;
    }

    public String[] getPublishers() {
        return publishers;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
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
        ServiceDescription other = (ServiceDescription) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /**
     * Builder for ServiceDescription.
     *
     * @author Marat Kamalov
     *
     */
    public static class ServiceDescriptionBuilder {
        private String name;
        private String url;
        private String serviceClassName;
        private String rmiServiceClassName;
        private String rmiName;
        private String annotationTemplateClassName;
        private boolean provideRuntimeContext;
        private boolean provideVariations = false;
        private Map<String, Object> configuration;
        private Collection<Module> modules;
        private DeploymentDescription deployment;
        private Set<String> publishers = new HashSet<>();
        private ResourceLoader resourceLoader;

        public ServiceDescriptionBuilder setPublishers(String[] publishers) {
            this.publishers = new HashSet<>();
            if (publishers != null) {
                for (String publisher : publishers) {
                    this.publishers.add(publisher);
                }
            }
            return this;
        }

        public ServiceDescriptionBuilder setResourceLoader(ResourceLoader resourceLoader) {
            Objects.requireNonNull(resourceLoader, "resourceLoader argument must not be null.");
            this.resourceLoader = resourceLoader;
            return this;
        }

        public void addPublisher(String key) {
            Objects.requireNonNull(key, "key argument must not be null.");
            if (this.publishers == null) {
                this.publishers = new HashSet<>();
            }
            this.publishers.add(key.toUpperCase());
        }

        /**
         * Sets annotation template class name
         *
         * @param annotationTemplateClassName
         */
        public ServiceDescriptionBuilder setAnnotationTemplateClassName(String annotationTemplateClassName) {
            this.annotationTemplateClassName = annotationTemplateClassName;
            return this;
        }

        /**
         * Sets name to the builder.
         *
         * @param name
         * @return
         */
        public ServiceDescriptionBuilder setName(String name) {
            Objects.requireNonNull(name, "name arg must not be null.");
            this.name = name;
            return this;
        }

        /**
         * Sets rmi name to the builder.
         *
         * @param name
         * @return
         */
        public ServiceDescriptionBuilder setRmiName(String rmiName) {
            Objects.requireNonNull(rmiName, "rmiName arg must not be null.");
            this.rmiName = rmiName;
            return this;
        }

        /**
         * Sets url to the builder.
         *
         * @param url
         * @return
         */
        public ServiceDescriptionBuilder setUrl(String url) {
            this.url = url;
            return this;
        }

        /**
         * Set a new set of modules to the builder.
         *
         * @param modules
         * @return
         */
        public ServiceDescriptionBuilder setModules(Collection<Module> modules) {
            if (modules == null) {
                this.modules = new HashSet<>(0);
            } else {
                this.modules = new HashSet<>(modules);
            }
            return this;
        }

        /**
         * Adds modules to the builder.
         *
         * @param modules
         * @return
         */
        public ServiceDescriptionBuilder addModules(Collection<Module> modules) {
            if (this.modules == null) {
                this.modules = new HashSet<>(modules);
            } else {
                this.modules.addAll(modules);
            }
            return this;
        }

        /**
         * Add module to the builder.
         *
         * @param module
         * @return
         */
        public ServiceDescriptionBuilder addModule(Module module) {
            if (this.modules == null) {
                this.modules = new HashSet<>(0);
            }
            if (module != null) {
                this.modules.add(module);
            }
            return this;
        }

        /**
         * Sets provideRuntimeContext to the builder.
         *
         * @param provideRuntimeContext
         * @return
         */
        public ServiceDescriptionBuilder setProvideRuntimeContext(boolean provideRuntimeContext) {
            this.provideRuntimeContext = provideRuntimeContext;
            return this;
        }

        /**
         * Sets class name to the builder. (Optional)
         *
         * @param serviceClassName
         * @return
         */
        public ServiceDescriptionBuilder setServiceClassName(String serviceClassName) {
            this.serviceClassName = serviceClassName;
            return this;
        }

        /**
         * Sets rmi class name to the builder. (Optional)
         *
         * @param rmiServiceClassName
         * @return
         */
        public ServiceDescriptionBuilder setRmiServiceClassName(String rmiServiceClassName) {
            this.rmiServiceClassName = rmiServiceClassName;
            return this;
        }

        /**
         * Sets flag that is responsible for variations support.
         *
         * @param provideVariations
         * @return
         */
        public ServiceDescriptionBuilder setProvideVariations(boolean provideVariations) {
            this.provideVariations = provideVariations;
            return this;
        }

        public ServiceDescriptionBuilder setDeployment(DeploymentDescription deployment) {
            this.deployment = deployment;
            return this;
        }

        public ServiceDescriptionBuilder addConfigurationProperty(String key, Object value) {
            if (this.configuration == null) {
                this.configuration = new HashMap<>();
            }
            this.configuration.put(key, value);
            return this;
        }

        public ServiceDescriptionBuilder setConfiguration(Map<String, Object> configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * Builds ServiceDesctiption.
         *
         * @return
         */
        public ServiceDescription build() {
            if (this.name == null) {
                throw new IllegalStateException("Field 'name' is required for building ServiceDescription");
            }
            if (this.resourceLoader == null) {
                throw new IllegalStateException("Field 'resourceLoader' is required for building ServiceDescription");
            }
            if (this.modules == null) {
                throw new IllegalStateException("Field 'modules' is required for building ServiceDescription");
            }
            if (this.deployment == null) {
                throw new IllegalStateException("Field 'deployment' is required for building ServiceDescription");
            }
            return new ServiceDescription(this);
        }

    }
}
