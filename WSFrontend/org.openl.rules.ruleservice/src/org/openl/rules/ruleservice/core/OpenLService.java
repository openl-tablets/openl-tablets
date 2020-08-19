package org.openl.rules.ruleservice.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Manifest;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.types.IOpenClass;

/**
 * Class designed for storing settings for service configuration and compiled service bean.
 * RuleServiceOpenLServiceInstantiationFactory is designed for build OpenLService instances.
 *
 * @author Marat Kamalov
 *
 */
public final class OpenLService {
    /**
     * Unique for service.
     */
    private final String name;
    private final String url;
    private final String servicePath;
    private String serviceClassName;
    private String rmiServiceClassName;
    private final String rmiName;
    private Class<?> serviceClass;
    private Class<?> rmiServiceClass;
    private Object serviceBean;
    private CompiledOpenClass compiledOpenClass;
    private final boolean provideRuntimeContext;
    private final boolean provideVariations;
    private final Collection<Module> modules;
    private final Set<String> publishers;
    private ClassLoader classLoader;
    private OpenLServiceInitializer initializer;
    private Throwable exception;
    private Map<String, String> urls = Collections.emptyMap();
    private final Manifest manifest;

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
     * @param name service name
     * @param url url
     * @param serviceClassName class name for service
     * @param provideRuntimeContext define is runtime context should be used
     * @param provideVariations define is variations should be supported
     * @param modules a list of modules for load
     */
    OpenLService(String name,
            String url,
            String servicePath,
            String serviceClassName,
            String rmiServiceClassName,
            String rmiName,
            boolean provideRuntimeContext,
            boolean provideVariations,
            Set<String> publishers,
            Collection<Module> modules,
            ClassLoader classLoader,
            Class<?> serviceClass,
            Manifest manifest) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.url = url;
        this.servicePath = servicePath;
        if (modules != null) {
            this.modules = Collections.unmodifiableCollection(modules);
        } else {
            this.modules = Collections.emptyList();
        }
        this.serviceClassName = serviceClassName;
        this.rmiServiceClassName = rmiServiceClassName;
        this.rmiName = rmiName;
        this.provideRuntimeContext = provideRuntimeContext;
        this.provideVariations = provideVariations;
        if (publishers != null) {
            this.publishers = Collections.unmodifiableSet(publishers);
        } else {
            this.publishers = Collections.emptySet();
        }

        this.classLoader = classLoader;
        this.serviceClass = serviceClass;
        this.manifest = manifest;
    }

    private OpenLService(OpenLServiceBuilder builder, OpenLServiceInitializer initializer) {
        this(builder.name,
            builder.url,
            builder.servicePath,
            builder.serviceClassName,
            builder.rmiServiceClassName,
            builder.rmiName,
            builder.provideRuntimeContext,
            builder.provideVariations,
            builder.publishers,
            builder.modules,
            builder.classLoader,
            builder.serviceClass,
            builder.manifest);
        this.initializer = Objects.requireNonNull(initializer, "initializer cannot be null");
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
     * Returns servicePath.
     *
     * @return servicePath
     */
    public String getServicePath() {
        return servicePath;
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

    void setServiceClassName(String serviceClassName) {
        this.serviceClassName = serviceClassName;
    }

    /**
     * Returns a rmi class name for service.
     *
     * @return
     */
    public String getRmiServiceClassName() throws RuleServiceInstantiationException {
        ensureInitialization();
        return rmiServiceClassName;
    }

    /**
     * Returns a rmi name for service.
     *
     * @return
     */
    public String getRmiName() {
        return rmiName;
    }

    void setRmiServiceClassName(String rmiServiceClassName) {
        this.rmiServiceClassName = rmiServiceClassName;
    }

    /**
     * Return provideRuntimeContext value. This value is define that service methods first argument is
     * IRulesRuntimeContext.
     *
     * @return isProvideRuntimeContext
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
     * Returns service class.
     *
     * @return
     */
    public Class<?> getServiceClass() throws RuleServiceInstantiationException {
        ensureInitialization();
        return serviceClass;
    }

    /**
     * Returns rmi service class.
     *
     * @return
     */
    public Class<?> getRmiServiceClass() throws RuleServiceInstantiationException {
        ensureInitialization();
        return rmiServiceClass;
    }

    void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    void setServiceClass(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
    }

    void setRmiServiceClass(Class<?> rmiServiceClass) {
        this.rmiServiceClass = rmiServiceClass;
    }

    public Object getServiceBean() throws RuleServiceInstantiationException {
        ensureInitialization();
        return serviceBean;
    }

    void setServiceBean(Object serviceBean) {
        this.serviceBean = serviceBean;
    }

    public CompiledOpenClass getCompiledOpenClass() {
        return compiledOpenClass;
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

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public Map<String, String> getUrls() {
        return urls;
    }

    public void setUrls(Map<String, String> urls) {
        this.urls = urls;
    }

    public Manifest getManifest() {
        return manifest;
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
        private String servicePath;
        private String serviceClassName;
        private String rmiServiceClassName;
        private String rmiName;
        private Class<?> serviceClass;
        private boolean provideRuntimeContext = false;
        private boolean provideVariations = false;
        private Collection<Module> modules;
        private Set<String> publishers;
        private ClassLoader classLoader;
        private Manifest manifest;

        public OpenLServiceBuilder setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public OpenLServiceBuilder setPublishers(Set<String> publishers) {
            if (publishers == null) {
                this.publishers = new HashSet<>(0);
            } else {
                this.publishers = publishers;
            }
            return this;
        }

        public OpenLServiceBuilder addPublishers(Set<String> publishers) {
            if (this.publishers == null) {
                this.publishers = new HashSet<>();
            }
            if (publishers != null) {
                this.publishers.addAll(publishers);
            }
            return this;
        }

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
         * Sets RMI class name to the builder.
         *
         * @param rmiServiceClassName
         * @return
         */
        public OpenLServiceBuilder setRmiServiceClassName(String rmiServiceClassName) {
            this.rmiServiceClassName = rmiServiceClassName;
            return this;
        }

        /**
         * Sets RMI name to the builder.
         *
         * @param rmiName
         * @return
         */
        public OpenLServiceBuilder setRmiName(String rmiName) {
            this.rmiName = rmiName;
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
         * Sets provideVariations flag to the builder. (Optional)
         *
         * @param provideVariations
         * @return
         */
        public OpenLServiceBuilder setProvideVariations(boolean provideVariations) {
            this.provideVariations = provideVariations;
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
         * Sets servicePath to the builder.
         *
         * @param servicePath
         * @return
         */
        public OpenLServiceBuilder setServicePath(String servicePath) {
            this.servicePath = servicePath;
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

        public OpenLServiceBuilder setManifest(Manifest manifest) {
            this.manifest = manifest;
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
            return new OpenLService(this, initializer);
        }
    }
}
