package org.openl.rules.ruleservice.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    private String interceptingTemplateClassName;
    private boolean provideRuntimeContext;
    private boolean useRuleServiceRuntimeContext;
    private boolean provideVariations;
    private Map<String, Object> configuration;
    private Collection<ModuleDescription> modules;
    private Set<ModuleDescription> modulesInService;
    private DeploymentDescription deployment;

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
    ServiceDescription(String name, String url, String serviceClassName, String interceptingTemplateClassName,
            boolean provideRuntimeContext, boolean useRuleServiceRuntimeContext, boolean provideVariations,
            Collection<ModuleDescription> modules, Set<ModuleDescription> modulesInService,
            Map<String, Object> configuration) {
        this.name = name;
        this.url = url;
        this.serviceClassName = serviceClassName;
        this.provideRuntimeContext = provideRuntimeContext;
        this.useRuleServiceRuntimeContext = useRuleServiceRuntimeContext;
        this.provideVariations = provideVariations;
        this.interceptingTemplateClassName = interceptingTemplateClassName;
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

        if (modulesInService != null) {
            this.modulesInService = Collections.unmodifiableSet(modulesInService);
        } else {
            this.modulesInService = Collections.emptySet();
        }

        if (!modules.isEmpty()) {
            ModuleDescription m = modules.iterator().next();
            this.deployment = new DeploymentDescription(m.getDeploymentName(), m.getDeploymentVersion());
        }
    }

    private ServiceDescription(ServiceDescriptionBuilder builder) {
        this(builder.name, builder.url, builder.serviceClassName, builder.interceptingTemplateClassName,
                builder.provideRuntimeContext, builder.useRuleServiceRuntimeContext, builder.provideVariations,
                builder.modules, builder.modulesInService, builder.configuration);
    }

    /**
     * Returns interceptor template class name
     * 
     * @return class name
     */
    public String getInterceptorTemplateClassName() {
        return interceptingTemplateClassName;
    }

    /**
     * Return useRuleServiceRuntimeContext
     * 
     * @return
     */
    public boolean isUseRuleServiceRuntimeContext() {
        return useRuleServiceRuntimeContext;
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
     * Returns provideRuntimeContext value. This value is define that service
     * methods first argument is IRulesRuntimeContext.
     * 
     * @return
     */
    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    /**
     * This flag defines whether variations will be supported or not.
     * 
     * @return <code>true</code> if variations should be injected in service
     *         class, and <code>false</code> otherwise.
     */
    public boolean isProvideVariations() {
        return provideVariations;
    }

    /**
     * Returns modules for the deployment.
     * 
     * @return a set of modules
     */
    public Collection<ModuleDescription> getModules() {
        return modules;
    }

    /**
     * Get module names in service
     * 
     * @return module names in service
     */
    public Set<ModuleDescription> getModulesInService() {
        return modulesInService;
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

    /** {@inheritDoc} */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        private String interceptingTemplateClassName;
        private boolean provideRuntimeContext;
        private boolean provideVariations = false;
        private boolean useRuleServiceRuntimeContext = false;
        private Map<String, Object> configuration;
        private Collection<ModuleDescription> modules;
        private Set<ModuleDescription> modulesInService;

        /**
         * Sets intercepting template class name
         * 
         * @param interceptingTemplateClassName
         */
        public ServiceDescriptionBuilder setInterceptingTemplateClassName(String interceptingTemplateClassName) {
            this.interceptingTemplateClassName = interceptingTemplateClassName;
            return this;
        }

        /**
         * Sets useRuleServiceRuntimeContext
         * 
         * @param useRuleServiceRuntimeContext
         */
        public ServiceDescriptionBuilder setUseRuleServiceRuntimeContext(boolean useRuleServiceRuntimeContext) {
            this.useRuleServiceRuntimeContext = useRuleServiceRuntimeContext;
            return this;
        }

        /**
         * Sets name to the builder.
         * 
         * @param name
         * @return
         */
        public ServiceDescriptionBuilder setName(String name) {
            if (name == null) {
                throw new IllegalArgumentException("name can't be null");
            }
            this.name = name;
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
        public ServiceDescriptionBuilder setModules(Collection<ModuleDescription> modules) {
            if (modules == null) {
                this.modules = new HashSet<ModuleDescription>(0);
            } else {
                this.modules = new HashSet<ModuleDescription>(modules);
            }
            return this;
        }

        /**
         * Adds modules to the builder.
         * 
         * @param modules
         * @return
         */
        public ServiceDescriptionBuilder addModules(Collection<ModuleDescription> modules) {
            if (this.modules == null) {
                this.modules = new HashSet<ModuleDescription>(modules);
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
        public ServiceDescriptionBuilder addModule(ModuleDescription module) {
            if (this.modules == null) {
                this.modules = new HashSet<ModuleDescription>(0);
            }
            if (module != null) {
                this.modules.add(module);
            }
            return this;
        }

        /**
         * Set a new set of modules in deployment to the builder.
         * 
         * @param modulesInDeployment
         * @return
         */
        public ServiceDescriptionBuilder setModulesInService(Collection<ModuleDescription> moduleNamesInService) {
            if (moduleNamesInService == null) {
                this.modulesInService = new HashSet<ModuleDescription>(0);
            } else {
                this.modulesInService = new HashSet<ModuleDescription>(moduleNamesInService);
            }
            return this;
        }

        /**
         * Adds modules in deployment to the builder.
         * 
         * @param modulesInDeployment
         * @return
         */
        public ServiceDescriptionBuilder addModulesInService(Collection<ModuleDescription> moduleNamesInService) {
            if (this.modulesInService == null) {
                this.modulesInService = new HashSet<ModuleDescription>(moduleNamesInService);
            } else {
                this.modulesInService.addAll(moduleNamesInService);
            }
            return this;
        }

        /**
         * Add module in deployment to the builder.
         * 
         * @param moduleInDeployment
         * @return
         */
        public ServiceDescriptionBuilder addModuleInService(ModuleDescription moduleInDeployment) {
            if (this.modulesInService == null) {
                this.modulesInService = new HashSet<ModuleDescription>(0);
            }
            if (moduleInDeployment != null) {
                this.modulesInService.add(moduleInDeployment);
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
         * Sets flag that is responsible for variations support.
         * 
         * @param provideVariations
         * @return
         */
        public ServiceDescriptionBuilder setProvideVariations(boolean provideVariations) {
            this.provideVariations = provideVariations;
            return this;
        }

        public ServiceDescriptionBuilder addConfigurationProperty(String key, Object value) {
            if (this.configuration == null) {
                this.configuration = new HashMap<String, Object>();
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
                throw new IllegalStateException("name is required field for building ServiceDescription");
            }
            if (this.modules == null) {
                throw new IllegalStateException("modules is required field for building ServiceDescription");
            }
            if (this.modulesInService == null) {
                throw new IllegalStateException("modulesInService is required field for building ServiceDescription");
            }
            if (modules.size() < modulesInService.size()) {
                throw new IllegalStateException(
                        "moduleNamesInService size cannot be greater than modules in deployment size");
            }
            return new ServiceDescription(this);
        }
    }
}
