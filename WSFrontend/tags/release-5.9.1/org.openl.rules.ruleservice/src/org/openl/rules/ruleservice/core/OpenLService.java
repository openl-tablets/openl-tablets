package org.openl.rules.ruleservice.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.openl.rules.project.model.Module;

/**
 * Class designed for storing settings for service configuration and compiled
 * service bean. RuleServiceOpenLServiceInstantiationFactory is designed for
 * build OpenLService instances.
 * 
 * @author Marat Kamalov
 * 
 */
public final class OpenLService {
    /**
     * Unique for service.
     */
    private String name;
    private String url;
    private String serviceClassName;
    private Class<?> serviceClass;
    private Object serviceBean;
    private boolean provideRuntimeContext = false;
    private Collection<Module> modules;

    /**
     * Main constructor.
     * 
     * @param name service name
     * @param url url
     * @param serviceClassName class name for service
     * @param provideRuntimeContext define is runtime context should be used
     * @param modules a list of modules for load
     */
    OpenLService(String name, String url, String serviceClassName, boolean provideRuntimeContext,
            Collection<Module> modules) {
        if (name == null) {
            throw new IllegalArgumentException("name arg can't be null");
        }
        this.name = name;
        this.url = url;
        if (modules != null) {
            this.modules = Collections.unmodifiableCollection(modules);
        } else {
            this.modules = Collections.emptyList();
        }
        this.serviceClassName = serviceClassName;
        this.provideRuntimeContext = provideRuntimeContext;
    }

    private OpenLService(OpenLServiceBuilder builder) {
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
     * @return service URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns unmodifiable collection of modules.
     * 
     * @return a collection of modules
     */
    public Collection<Module> getModules() {
        return modules;
    }

    /**
     * Returns a class name for service.
     * 
     * @return
     */
    public String getServiceClassName() {
        return serviceClassName;
    }

    /**
     * Return provideRuntimeContext value. This value is define that service
     * methods first argument is IRulesRuntimeContext.
     * 
     * @return isProvideRuntimeContext
     */
    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    /**
     * Returns service class.
     * 
     * @return
     */
    public Class<?> getServiceClass() {
        return serviceClass;
    }

    void setServiceClass(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
    }

    public Object getServiceBean() {
        return serviceBean;
    }

    void setServiceBean(Object serviceBean) {
        this.serviceBean = serviceBean;
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
        OpenLService other = (OpenLService) obj;
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
     * OpenLService builder.
     * 
     * @author Marat Kamalov
     * 
     */
    public static class OpenLServiceBuilder {
        private String name;
        private String url;
        private String serviceClassName;
        private boolean provideRuntimeContext = false;
        private Collection<Module> modules;

        /**
         * Sets name to the builder.
         * 
         * @param name
         * @return
         */
        public OpenLServiceBuilder setName(String name) {
            if (name == null) {
                throw new IllegalArgumentException("name arg can't be null");
            }
            this.name = name;
            return this;
        }

        /**
         * Sets class name to the builder.
         * 
         * @param serviceClassName
         * @return
         */
        public OpenLServiceBuilder setServiceClassName(String serviceClassName) {
            this.serviceClassName = serviceClassName;
            return this;
        }

        /**
         * Sets provideRuntimeContext to the builder.
         * 
         * @param provideRuntimeContext
         * @return
         */
        public OpenLServiceBuilder setProvideRuntimeContext(boolean provideRuntimeContext) {
            this.provideRuntimeContext = provideRuntimeContext;
            return this;
        }

        /**
         * Sets a new set of modules to the builder.
         * 
         * @param modules
         * @return
         */
        public OpenLServiceBuilder setModules(Collection<Module> modules) {
            if (modules == null) {
                modules = new ArrayList<Module>(0);
            } else {
                this.modules = new ArrayList<Module>(modules);
            }
            return this;
        }

        /**
         * Add modules to the builder.
         * 
         * @param modules
         * @return
         */
        public OpenLServiceBuilder addModules(Collection<Module> modules) {
            if (modules == null) {
                this.modules = new ArrayList<Module>(0);
            }
            this.modules.addAll(modules);
            return this;
        }

        /**
         * Adds module to the builder.
         * 
         * @param module
         * @return
         */
        public OpenLServiceBuilder addModule(Module module) {
            if (modules == null) {
                this.modules = new ArrayList<Module>(0);
            }
            if (module != null) {
                this.modules.add(module);
            }
            return this;
        }

        /**
         * Sets url to the builder.
         * 
         * @param url
         * @return
         */
        public OpenLServiceBuilder setUrl(String url) {
            this.url = url;
            return this;
        }

        /**
         * Builds OpenLService.
         * 
         * @return
         */
        public OpenLService build() {
            if (name == null) {
                throw new IllegalStateException("name is required field for building ServiceDescription");
            }
            return new OpenLService(this);
        }
    }
}
