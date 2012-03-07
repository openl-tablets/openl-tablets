package org.openl.rules.ruleservice.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

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
    private boolean provideRuntimeContext;
    private Collection<ModuleDescription> modules;

    /**
     * Main constructor.
     * 
     * @param name
     * @param url
     * @param serviceClassName
     * @param provideRuntimeContext
     * @param modules
     */
    ServiceDescription(String name, String url, String serviceClassName, boolean provideRuntimeContext,
            Collection<ModuleDescription> modules) {
        this.name = name;
        this.url = url;
        this.serviceClassName = serviceClassName;
        this.provideRuntimeContext = provideRuntimeContext;
        if (modules != null) {
            this.modules = Collections.unmodifiableCollection(modules);
        } else {
            this.modules = Collections.emptySet();
        }
    }

    private ServiceDescription(ServiceDescriptionBuilder builder) {
        this(builder.name, builder.url, builder.serviceClassName, builder.provideRuntimeContext, builder.modules);
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
     * Return provideRuntimeContext value. This value is define that service
     * methods first argument is IRulesRuntimeContext.
     * 
     * @return
     */
    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    /**
     * Return modules for the service.
     * 
     * @return a set of modules
     */
    public Collection<ModuleDescription> getModules() {
        return modules;
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
        private boolean provideRuntimeContext;
        private Collection<ModuleDescription> modules;

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
            if (modules == null) {
                this.modules = new HashSet<ModuleDescription>(0);
            }
            this.modules.addAll(modules);
            return this;
        }

        /**
         * Add module to the builder.
         * 
         * @param module
         * @return
         */
        public ServiceDescriptionBuilder addModule(ModuleDescription module) {
            if (modules == null) {
                this.modules = new HashSet<ModuleDescription>(0);
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
         * Sets class name to the builder.
         * 
         * @param serviceClassName
         * @return
         */
        public ServiceDescriptionBuilder setServiceClassName(String serviceClassName) {
            this.serviceClassName = serviceClassName;
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
            return new ServiceDescription(this);
        }
    }
}
