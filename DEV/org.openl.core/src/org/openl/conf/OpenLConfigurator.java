/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.io.File;
import java.util.Properties;

import org.openl.util.Log;

/**
 *
 * This class is responsible for configuration of a particular OpenL instance
 * designated by it's instance_name.
 *
 *
 *
 * @author snshor
 *
 */

public class OpenLConfigurator extends Configurator {

    public static final String OPENL_ALT_CONFIG_ROOT = "lang.config";

    public static final String OPENL_DEFAULT_PROPERTY_FILE_NAME = "OpenL.properties";
    public static final String OPENL_PROPERTY_FILE_PROPERTY = "properties.file";

    public static final String DEFAULT_BUILDER_CLASS_PROPERTY = "org.openl.builderclass";
    public static final String BUILDER_CLASS = ".builderclass";

    public static final String DEFAULT_BUILDER_CLASS_PATH_PROPERTY = "org.openl.builder.classpath";
    public static final String BUILDER_CLASS_PATH = ".builder.classpath";
    // static final public String DEFAULT_BUILDER_CLASS_NAME =

    public static final String OPENL_BUILDER = "OpenLBuilder";

    static PropertyFileLoader OPENL_GLOBAL_PROPERTY_PROVIDER = new PropertyFileLoader(OPENL_DEFAULT_PROPERTY_FILE_NAME,
            "org.openl." + OPENL_PROPERTY_FILE_PROPERTY, new ConfigurableResourceContext(null), null);

    // ClassLoaderFactory clFactory = new ClassLoaderFactory();

    public synchronized IOpenLBuilder getBuilder(String openlName, IUserContext ucxt) throws OpenConfigurationException {

        // ClassLoaderFactory.setCurrentFactory(clFactory);

        String userHome = ucxt.getUserHome();
        String altHome = makeAlternativeHome(userHome);

        String[] homes = altHome == null ? new String[] { userHome } : new String[] { userHome, altHome };

        ConfigurableResourceContext cxt = new ConfigurableResourceContext(ucxt.getUserClassLoader(), homes);

        PropertyFileLoader propertyLoader = new PropertyFileLoader(openlName + "." + OPENL_DEFAULT_PROPERTY_FILE_NAME,
                openlName + "." + OPENL_PROPERTY_FILE_PROPERTY, cxt, new PropertyFileLoader(
                        OPENL_DEFAULT_PROPERTY_FILE_NAME, "org.openl." + OPENL_PROPERTY_FILE_PROPERTY, cxt, null));

        Properties pp = propertyLoader.getProperties();
        pp = mergeProperties(pp, ucxt.getUserProperties());
        if (pp != PropertyFileLoader.NO_PROPERTIES) {
            cxt.setProperties(pp);
        }

        try {
            IOpenLBuilder builder = makeBuilder(openlName, cxt, ucxt);
            builder.setContexts(cxt, ucxt);

            return builder;
        } catch (Exception ex) {
            throw new OpenConfigurationException("Error creating builder: ", null, ex);
        }
    }

    String getBuilderClassName(String openl, IConfigurableResourceContext cxt) {

        String builderClassName = cxt.findProperty(openl + BUILDER_CLASS);
        if (builderClassName == null) {
            builderClassName = cxt.findProperty(DEFAULT_BUILDER_CLASS_PROPERTY);
        }

        return builderClassName;
    }

    String getBuilderClassPath(String openl, IConfigurableResourceContext cxt) {

        String builderClassPath = cxt.findProperty(openl + BUILDER_CLASS_PATH);
        if (builderClassPath == null) {
            builderClassPath = cxt.findProperty(DEFAULT_BUILDER_CLASS_PATH_PROPERTY);
        }

        return builderClassPath;
    }

    public String makeAlternativeHome(String userHome) {
        try {
            return new File(userHome + "/../" + OPENL_ALT_CONFIG_ROOT).getCanonicalPath();
        } catch (Throwable t) {
            return null;
        }
    }

    IOpenLBuilder makeBuilder(String openl, IConfigurableResourceContext cxt, IUserContext ucxt) throws Exception {
        String builderClassName = getBuilderClassName(openl, cxt);
        String builderClassPath = getBuilderClassPath(openl, cxt);
        if (builderClassName != null) {
            return makeBuilderInstance(builderClassName, builderClassPath, ucxt);
        }

        try {
            builderClassName = openl + "." + OPENL_BUILDER;
            IOpenLBuilder bb = makeBuilderInstance(builderClassName, builderClassPath, ucxt);
            return bb;
        } catch (Exception e) {
            Log.error("Can not build " + openl + " using cp: " + builderClassPath + " UCXT: " + ucxt, e);
            throw e;
        }

        // builderClassName = DEFAULT_BUILDER_CLASS_NAME;
        // return makeBuilderInstance(builderClassName, builderClassPath, ucxt);

    }

    IOpenLBuilder makeBuilderInstance(String builderClassName, String builderClassPath, IUserContext ucxt)
            throws Exception {

        ClassLoader cl = ClassLoaderFactory.getOpenlCoreLoader(ucxt.getUserClassLoader());
        if (builderClassPath != null) {
            cl = ClassLoaderFactory.createUserClassloader("builder.classloader:", builderClassPath, ClassLoaderFactory
                    .getOpenlCoreLoader(ucxt.getUserClassLoader()), ucxt);
        }

        return (IOpenLBuilder) ClassFactory.newInstanceForName(builderClassName, cl);
    }

    Properties mergeProperties(Properties properties, Properties defaults) {
        Properties result = null;

        if (defaults != null && defaults != PropertyFileLoader.NO_PROPERTIES) {
            result = new Properties(defaults);
        }

        if (properties != null && properties != PropertyFileLoader.NO_PROPERTIES) {
            if (result == null) {
                result = new Properties();
            }

            result.putAll(properties);
        }

        return result != null ? result : PropertyFileLoader.NO_PROPERTIES;
    }

}
