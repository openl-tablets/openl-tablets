/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl;

import org.openl.conf.IOpenLBuilder;
import org.openl.conf.IUserContext;
import org.openl.conf.OpenConfigurationException;
import org.openl.conf.OpenLConfigurator;
import org.openl.conf.UserContext;

/**
 * This class describes OpenL engine context abstraction that used during
 * compilation process.
 * 
 * The class OpenL implements both factory(static) methods for creating OpenL
 * instances and actual OpenL functionality. Each instance of OpenL should be
 * considered as a Language Configuration(LC). You may have as many LCs in your
 * application as you want. Current OpenL architecture allows to have different
 * OpenL configurations in separate class loaders, so they will not interfere
 * with each other. It allows, for example, to have 2 LCs using different SAX or
 * DOM parser implementation.
 * 
 * The actual work is done by class OpenLConfigurator.
 * 
 * @see OpenLConfigurator
 * @author snshor
 */
public class OpenL {
    public static final String OPENL_J_NAME = "org.openl.j";
    public static final String OPENL_JAVA_NAME = "org.openl.rules.java";
    public static final String OPENL_JAVA_CE_NAME = "org.openl.rules.java.ce"; 
    public static final String OPENL_JAVA_RULE_NAME = "org.openl.xls";    
    

    private static final String DEFAULT_USER_HOME = ".";

    private static OpenLConfigurator config = new OpenLConfigurator();

    private IOpenParser parser;

    private IOpenBinder binder;

    private IOpenVM vm;

    private ICompileContext compileContext;

    private String name;

    public OpenL() {
    }

    /**
     * Change default OpenLConfigurator implementation to another.
     * 
     * @param config new OpenLConfigurator
     */
    public static void setConfig(OpenLConfigurator config) {
        OpenL.config = config;
    }

    /**
     * Gets instance of <code>OpenL</code> with given name.
     * 
     * @param name OpenL name
     * @return instance of OpenL
     * @throws OpenConfigurationException
     */
    //TODO: Do not use this method! Should be removed!    
    public static synchronized OpenL getInstance(String name) throws OpenConfigurationException {
        return getInstance(name, new UserContext(config.getClassLoader(), DEFAULT_USER_HOME));
    }
    
    /**
     * Gets an instance of OpenL. Each instance is cached with name and user
     * context as it's key. To remove cached instance use #remove method
     * 
     * @see #remove
     * @see IUserContext
     * 
     * @param name IOpenL name, for example org.openl.java12.v101
     * @param userContext user context
     * @return instance of IOpenL
     * @throws OpenConfigurationException
     */
    public static synchronized OpenL getInstance(String name,
            IUserContext userContext) throws OpenConfigurationException {
        IOpenLBuilder builder = config.getBuilder(name, userContext);
        return getInstance(name, userContext, builder);
    }

    /**
     * Gets an instance of OpenL. Each instance is cached with name and user
     * context as it's key.
     * 
     * @see IUserContext
     * 
     * @param name IOpenL name
     * @param userContext user context
     * @param builder {@link IOpenLBuilder} instance which used to build new
     *            instance of OpenL if that doesn't exist
     * @return instance of IOpenL
     * @throws OpenConfigurationException
     */
    public static synchronized OpenL getInstance(String name, IUserContext userContext, IOpenLBuilder builder) {
        OpenL openl = userContext.getOpenL(name);
        if (openl == null) {
            openl = builder.build(name);
            userContext.registerOpenL(name, openl);
        }

        return openl;
    }

    /**
     * Gets name of OpenL instance.
     * 
     * @return name string
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name of OpenL instance.
     * 
     * @param name name string
     */
    public void setName(String name) {
        this.name = name;
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

    /**
     * Gets compilation context.
     * 
     * @return {@link ICompileContext} instance
     */
    public ICompileContext getCompileContext() {
        return compileContext;
    }

    /**
     * Sets compilation context.
     * 
     * @param compileContext {@link ICompileContext} instance
     */
    public void setCompileContext(ICompileContext compileContext) {
        this.compileContext = compileContext;
    }

    @Override
    public String toString() {
        return getName();
    }
}