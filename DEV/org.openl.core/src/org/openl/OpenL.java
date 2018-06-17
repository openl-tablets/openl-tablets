/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl;

import java.util.Map;

import org.apache.commons.collections4.map.ReferenceMap;
import org.openl.cache.GenericKey;
import org.openl.conf.IOpenLBuilder;
import org.openl.conf.IOpenLConfiguration;
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
    // TODO: Merge org.openl.xls and org.openl.xls.sequential implementations and rename OpenL name back to "org.openl.xls".
    public static final String OPENL_JAVA_RULE_NAME = "org.openl.xls.sequential";

    private static final String DEFAULT_USER_HOME = ".";

    private static OpenLConfigurator config = new OpenLConfigurator();

    // Soft references to values are used to prevent memory leak
    private static Map<Object, OpenL> openLCache = new ReferenceMap<Object, OpenL>();

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
    public static OpenL getInstance(String name) throws OpenConfigurationException {
        return getInstance(name, config.getClassLoader());
    }

    /**
     * Gets instance of <code>OpenL</code> with given name and that use the
     * given class loader.
     * 
     * @param name OpenL name
     * @param classLoader class loader that associated with OpenL instance and
     *            used for resource loading
     * @return OpenL instance
     * @throws OpenConfigurationException
     * 
     * @see IOpenLConfiguration
     * @see IUserContext
     */
    public static synchronized OpenL getInstance(String name, ClassLoader classLoader)
            throws OpenConfigurationException {

        return getInstance(name, new UserContext(classLoader, DEFAULT_USER_HOME));
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
    public static synchronized OpenL getInstance(String name, IUserContext userContext)
            throws OpenConfigurationException {

        Object key = GenericKey.getInstance(name, userContext);
        OpenL openl = openLCache.get(key);

        if (openl == null) {
            IOpenLBuilder builder = config.getBuilder(name, userContext);
            openl = createInstance(name, userContext, builder);

            openLCache.put(key, openl);
        }

        return openl;
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

        Object key = GenericKey.getInstance(name, userContext);
        OpenL openl = openLCache.get(key);

        if (openl == null) {
            openl = createInstance(name, userContext, builder);

            openLCache.put(key, openl);
        }

        return openl;
    }

    /**
     * Creates new instance of OpenL.
     * 
     * @param name name of OpenL
     * @param userContext {@link IUserContext} instance
     * @param builder {@link IOpenLBuilder} instance which used to build new
     *            instance of OpenL if that doesn't exist
     * @return new instance of OpenL
     */
    private static OpenL createInstance(String name, IUserContext userContext, IOpenLBuilder builder) {
    	
//    	System.out.println("!!!!!!!!!!!!!!!!!!!  Creating instance " + name + "    ----------------------------");
    	
        OpenL openl = builder.build(name);
//        openl.setName(name);

        return openl;
    }

    /**
     * Removes instance of OpenL with given name from cache.
     * 
     * @param name OpenL name
     * @return removed OpenL instance
     * @throws OpenConfigurationException
     */
    public static synchronized OpenL remove(String name) throws OpenConfigurationException {
        return remove(name, config.getClassLoader());
    }

    /**
     * Removes instance of OpenL with given name and class loader from cache.
     * 
     * @param name OpenL name
     * @param classLoader class loader that associated with OpenL instance and
     *            used for resource loading
     * @return removed OpenL instance
     * @throws OpenConfigurationException
     * 
     * @see IOpenLConfiguration
     * @see IUserContext
     */
    public static synchronized OpenL remove(String name, ClassLoader classLoader) throws OpenConfigurationException {
        return remove(name, new UserContext(classLoader, DEFAULT_USER_HOME));
    }

    /**
     * Removes instance of OpenL with given name using specified user context.
     * 
     * @param name OpenL name
     * @param userContext user context that used to find appropriate instance of
     *            OpenL
     * @return removed OpenL instance
     * 
     * @see IUserContext
     */
    public static synchronized OpenL remove(String name, IUserContext userContext) {

        Object key = GenericKey.getInstance(name, userContext);
        /*OpenL openl = openlCache.get(key);

        if (openl == null) {
            return null;
        }*/

        return openLCache.remove(key);

        //return openl;
    }

    /**
     * Resets OpenL internal cache.
     */
    //FIXME: multithreading issue: users can reset foreign OpenL calculation
    public static synchronized void reset() {
        //openlCache = new HashMap<Object, OpenL>();
        openLCache.clear();
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