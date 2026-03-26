package org.openl.rules.ruleservice.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ConfigurableApplicationContext;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.types.IOpenClass;

/**
 * Class designed for storing settings for service configuration and compiled service bean.
 * RuleServiceOpenLServiceInstantiationFactory is designed for build OpenLService instances.
 *
 * @author Marat Kamalov
 */
public final class OpenLService {
    /**
     * Unique for service.
     */
    @Getter
    private final String name;
    @Getter
    private final String url;
    @Getter
    private final String deployPath;
    @Setter(AccessLevel.PACKAGE)
    private String serviceClassName;
    @Setter(AccessLevel.PACKAGE)
    private Class<?> serviceClass;
    @Setter(AccessLevel.PACKAGE)
    private Object serviceBean;
    @Getter
    private CompiledOpenClass compiledOpenClass;
    @Getter
    private final boolean provideRuntimeContext;
    private final Collection<Module> modules;
    private final Set<String> publishers;
    @Setter(AccessLevel.PACKAGE)
    private ClassLoader classLoader;
    private OpenLServiceInitializer initializer;
    @Getter
    @Setter
    private Throwable exception;
    @Getter
    @Setter
    private Map<String, String> urls = Collections.emptyMap();
    @Getter
    private final DeploymentDescription deployment;
    @Getter
    @Setter
    private ConfigurableApplicationContext serviceContext;

    /**
     * Returns service classloader
     *
     * @return classLoader
     */
    public ClassLoader getClassLoader() throws RuleServiceInstantiationException {
        ensureInitialization();
        return classLoader;
    }

    /**
     * Main constructor.
     *
     * @param name                  service name
     * @param url                   url
     * @param serviceClassName      class name for service
     * @param provideRuntimeContext define is runtime context should be used
     * @param modules               a list of modules for load
     */
    OpenLService(String name,
                 String url,
                 String deployPath,
                 String serviceClassName,
                 boolean provideRuntimeContext,
                 Set<String> publishers,
                 Collection<Module> modules,
                 ClassLoader classLoader,
                 Class<?> serviceClass,
                 DeploymentDescription deployment) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.url = url;
        this.deployPath = deployPath;
        if (modules != null) {
            this.modules = Collections.unmodifiableCollection(modules);
        } else {
            this.modules = Collections.emptyList();
        }
        this.serviceClassName = serviceClassName;
        this.provideRuntimeContext = provideRuntimeContext;
        if (publishers != null) {
            this.publishers = Collections.unmodifiableSet(publishers);
        } else {
            this.publishers = Collections.emptySet();
        }

        this.classLoader = classLoader;
        this.serviceClass = serviceClass;
        this.deployment = deployment;
    }

    private OpenLService(OpenLServiceBuilder builder, OpenLServiceInitializer initializer) {
        this(builder.name,
                builder.url,
                builder.deployPath,
                builder.serviceClassName,
                builder.provideRuntimeContext,
                builder.publishers,
                builder.modules,
                null,
                builder.serviceClass,
                builder.deployment);
        this.initializer = Objects.requireNonNull(initializer, "initializer cannot be null");
    }

    /**
     * Returns service publishers.
     *
     * @return service publishers
     */
    public Collection<String> getPublishers() {
        if (publishers == null) {
            return Collections.emptyList();
        }
        return publishers;
    }

    /**
     * Returns unmodifiable collection of modules.
     *
     * @return a collection of modules
     */
    public Collection<Module> getModules() {
        if (modules == null) {
            return Collections.emptyList();
        }
        return modules;
    }

    private void ensureInitialization() throws RuleServiceInstantiationException {
        initializer.ensureInitialization(this);
    }

    /**
     * Returns a class name for service.
     *
     * @return
     */
    public String getServiceClassName() throws RuleServiceInstantiationException {
        ensureInitialization();
        return serviceClassName;
    }

    /**
     * Returns service class.
     *
     * @return
     */
    public Class<?> getServiceClass() throws RuleServiceInstantiationException {
        ensureInitialization();
        return serviceClass;
    }

    public Object getServiceBean() throws RuleServiceInstantiationException {
        ensureInitialization();
        return serviceBean;
    }

    void setCompiledOpenClass(CompiledOpenClass compiledOpenClass) {
        this.compiledOpenClass = compiledOpenClass;
        // bad practice. logic moved from another place
        compiledOpenClass.throwErrorExceptionsIfAny();
    }

    public IOpenClass getOpenClass() throws RuleServiceInstantiationException {
        ensureInitialization();
        return compiledOpenClass != null ? compiledOpenClass.getOpenClass() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (deployPath == null ? 0 : deployPath.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
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
        OpenLService other = (OpenLService) obj;
        if (deployPath == null) {
            if (other.deployPath != null) {
                return false;
            }
        } else if (!deployPath.equals(other.deployPath)) {
            return false;
        }
        return true;
    }

    /**
     * OpenLService builder.
     *
     * @author Marat Kamalov
     */
    public static class OpenLServiceBuilder {
        private String name;
        private String url;
        private String deployPath;
        private String serviceClassName;
        private Class<?> serviceClass;
        private boolean provideRuntimeContext = false;
        private Collection<Module> modules;
        private Set<String> publishers;
        private DeploymentDescription deployment;

        public OpenLServiceBuilder addPublisher(String publisher) {
            if (this.publishers == null) {
                this.publishers = new HashSet<>();
            }
            if (publisher != null) {
                this.publishers.add(publisher);
            }
            return this;
        }

        /**
         * Sets name to the builder.
         *
         * @param name
         * @return
         */
        public OpenLServiceBuilder setName(String name) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
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
                this.modules = new ArrayList<>(0);
            } else {
                this.modules = new ArrayList<>(modules);
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
            if (this.modules == null) {
                this.modules = new ArrayList<>();
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
            if (this.modules == null) {
                this.modules = new ArrayList<>();
            }
            if (module != null) {
                this.modules.add(module);
            }
            return this;
        }

        /**
         * Sets deployPath to the builder.
         *
         * @param deployPath
         * @return
         */
        public OpenLServiceBuilder setDeployPath(String deployPath) {
            this.deployPath = deployPath;
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

        public OpenLServiceBuilder setServiceClass(Class<?> serviceClass) {
            this.serviceClass = serviceClass;
            return this;
        }

        public OpenLServiceBuilder setDeployment(DeploymentDescription deployment) {
            this.deployment = deployment;
            return this;
        }

        /**
         * Builds OpenLService.
         *
         * @return
         */
        public OpenLService build(OpenLServiceInitializer initializer) {
            if (name == null) {
                throw new IllegalStateException("Field 'name' is required for building ServiceDescription.");
            }
            if (deployPath == null) {
                throw new IllegalStateException("Field 'deployPath' is required for building ServiceDescription.");
            }
            if (deployment == null) {
                throw new IllegalStateException("Field 'deployment' is required for building ServiceDescription.");
            }
            return new OpenLService(this, initializer);
        }
    }
}
