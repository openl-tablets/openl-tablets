package org.openl.rules.ruleservice.core;

import java.util.*;
import java.util.jar.Manifest;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.RulesDeploy;

/**
 * Class designed for storing service info.
 *
 * Immutable.
 *
 * @author Marat Kamalov
 *
 */
public final class ServiceDescription {
    private final String name;
    private final String url;
    private final String servicePath;
    private final String serviceClassName;
    private final String rmiServiceClassName;
    private final String rmiName;
    private final String annotationTemplateClassName;
    private final boolean provideRuntimeContext;
    private final boolean provideVariations;
    private final Map<String, Object> configuration;
    private final Collection<Module> modules;
    private final DeploymentDescription deployment;
    private final String[] publishers;
    private final ResourceLoader resourceLoader;
    private final Manifest manifest;
    private final RulesDeploy rulesDeploy;

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
            String servicePath,
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
            ResourceLoader resourceLoader,
            Manifest manifest,
            RulesDeploy rulesDeploy) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader cannot be null");
        this.url = url;
        this.servicePath = servicePath;
        this.serviceClassName = serviceClassName;
        this.provideRuntimeContext = provideRuntimeContext;
        this.rmiServiceClassName = rmiServiceClassName;
        this.rmiName = rmiName;
        this.provideVariations = provideVariations;
        this.annotationTemplateClassName = annotationTemplateClassName;
        this.rulesDeploy = rulesDeploy;
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
        this.manifest = manifest;
    }

    private ServiceDescription(ServiceDescriptionBuilder builder) {
        this(builder.name,
            builder.url,
            builder.servicePath,
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
            builder.resourceLoader,
            builder.manifest,
            builder.rulesDeploy);
    }

    public RulesDeploy getRulesDeploy() {
        return rulesDeploy;
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
     * Returns service servicePath.
     *
     * @return
     */
    public String getServicePath() {
        return servicePath;
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

    public Manifest getManifest() {
        return manifest;
    }

    public String[] getPublishers() {
        if (publishers == null) {
            return new String[] {};
        }
        return publishers;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
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

        if (servicePath == null) {
            if (other.servicePath != null) {
                return false;
            }
        } else if (!servicePath.equals(other.servicePath)) {
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
        private String servicePath;
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
        private Manifest manifest;
        private RulesDeploy rulesDeploy;

        public ServiceDescriptionBuilder setResourceLoader(ResourceLoader resourceLoader) {
            this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader cannot be null");
            return this;
        }

        public ServiceDescriptionBuilder setPublishers(String[] publishers) {
            this.publishers = new HashSet<>();
            if (publishers != null) {
                Collections.addAll(this.publishers, publishers);
            }
            return this;
        }

        public ServiceDescriptionBuilder setRulesDeploy(RulesDeploy rulesDeploy) {
            this.rulesDeploy = rulesDeploy;
            return this;
        }

        public void addPublisher(String publisher) {
            Objects.requireNonNull(publisher, "publisher cannot be null");
            if (this.publishers == null) {
                this.publishers = new HashSet<>();
            }
            this.publishers.add(publisher.toUpperCase());
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
            this.name = Objects.requireNonNull(name, "name cannot be null");
            return this;
        }

        /**
         * Sets rmi name to the builder.
         *
         * @param name
         * @return
         */
        public ServiceDescriptionBuilder setRmiName(String rmiName) {
            this.rmiName = Objects.requireNonNull(rmiName, "rmiName cannot be null");
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
         * Sets servicePath to the builder.
         *
         * @param servicePath
         * @return
         */
        public ServiceDescriptionBuilder setServicePath(String servicePath) {
            this.servicePath = servicePath;
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

        public ServiceDescriptionBuilder setManifest(Manifest manifest) {
            this.manifest = manifest;
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
