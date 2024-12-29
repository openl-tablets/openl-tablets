package org.openl;

import org.openl.conf.IOpenLBuilder;
import org.openl.conf.IUserContext;
import org.openl.conf.OpenLConfigurationException;
import org.openl.conf.UserContext;

/**
 * This class describes OpenL engine context abstraction that used during compilation process.
 * <p>
 * The class OpenL implements both factory(static) methods for creating OpenL instances and actual OpenL functionality.
 * Each instance of OpenL should be considered as a Language Configuration(LC). You may have as many LCs in your
 * application as you want. Current OpenL architecture allows to have different OpenL configurations in separate class
 * loaders, so they will not interfere with each other. It allows, for example, to have 2 LCs using different SAX or DOM
 * parser implementation.
 * <p>
 *
 * @author snshor
 */
public class OpenL {
    public static final String OPENL_J_NAME = "org.openl.j";
    public static final String OPENL_JAVA_RULE_NAME = "org.openl.xls";

    private static final String DEFAULT_USER_HOME = ".";

    private IOpenParser parser;

    private IOpenBinder binder;

    private IOpenVM vm;

    public OpenL() {
    }

    /**
     * Gets instance of <code>OpenL</code> with given name.
     *
     * @param name OpenL name
     * @return instance of OpenL
     * @throws OpenLConfigurationException
     */
    // TODO: Do not use this method! Should be removed!
    public static synchronized OpenL getInstance(String name) {
        return getInstance(name, new UserContext(OpenL.class.getClassLoader(), DEFAULT_USER_HOME));
    }

    /**
     * Gets an instance of OpenL. Each instance is cached with name and user context as it's key. To remove cached
     * instance use #remove method
     *
     * @param name        IOpenL name, for example org.openl.java12.v101
     * @param userContext user context
     * @return instance of IOpenL
     * @throws OpenLConfigurationException
     * @see IUserContext
     */
    public static synchronized OpenL getInstance(String name, IUserContext userContext) {

        try {
            var builderClassName = name + "." + "OpenLBuilder";
            Class<?> builderClass;
            try {
                builderClass = userContext.getUserClassLoader().loadClass(builderClassName);
            } catch (Exception ignored) {
                builderClass = Class.forName(builderClassName);
            }

            var builder = (IOpenLBuilder) builderClass.getDeclaredConstructor().newInstance();
            return getInstance(name, userContext, builder);
        } catch (Exception ex) {
            throw new OpenLConfigurationException("Error creating builder: ", ex);
        }
    }

    /**
     * Gets an instance of OpenL. Each instance is cached with name and user context as it's key.
     *
     * @param name        IOpenL name
     * @param userContext user context
     * @param builder     {@link IOpenLBuilder} instance which used to build new instance of OpenL if that does not exist
     * @return instance of IOpenL
     * @throws OpenLConfigurationException
     * @see IUserContext
     */
    public static synchronized OpenL getInstance(String name, IUserContext userContext, IOpenLBuilder builder) {
        OpenL openl = userContext.getOpenL(name);
        if (openl == null) {
            openl = builder.build(userContext);
            userContext.registerOpenL(name, openl);
        }
        return openl;
    }

    /**
     * Gets parser that configured for current OpenL instance.
     *
     * @return {@link IOpenParser} instance
     */
    public IOpenParser getParser() {
        return parser;
    }

    /**
     * Sets parser to current OpenL instance.
     *
     * @param parser {@link IOpenParser} instance
     */
    public void setParser(IOpenParser parser) {
        this.parser = parser;
    }

    /**
     * Gets virtual machine which used during rules execution.
     *
     * @return {@link IOpenVM} instance
     */
    public IOpenVM getVm() {
        return vm;
    }

    /**
     * Sets virtual machine.
     *
     * @param openVM {@link IOpenVM} instance
     */
    public void setVm(IOpenVM openVM) {
        vm = openVM;
    }

    /**
     * Gets binder that configured for current OpenL instance.
     *
     * @return {@link IOpenBinder} instance
     */
    public IOpenBinder getBinder() {
        return binder;
    }

    /**
     * Sets binder to current OpenL instance.
     *
     * @param binder {@link IOpenBinder} instance
     */
    public void setBinder(IOpenBinder binder) {
        this.binder = binder;
    }
}