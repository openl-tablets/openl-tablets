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

    public static final String OPENL_BUILDER = "OpenLBuilder";

    public synchronized IOpenLBuilder getBuilder(String openlName, IUserContext ucxt) throws OpenConfigurationException {

        String userHome = ucxt.getUserHome();
        String altHome = makeAlternativeHome(userHome);

        String[] homes = altHome == null ? new String[] { userHome } : new String[] { userHome, altHome };

        ConfigurableResourceContext cxt = new ConfigurableResourceContext(ucxt.getUserClassLoader(), homes);

        PropertyFileLoader propertyLoader = new PropertyFileLoader(openlName + "." + OPENL_DEFAULT_PROPERTY_FILE_NAME,
                openlName + "." + OPENL_PROPERTY_FILE_PROPERTY, cxt, new PropertyFileLoader(
                        OPENL_DEFAULT_PROPERTY_FILE_NAME, "org.openl." + OPENL_PROPERTY_FILE_PROPERTY, cxt, null));

        Properties pp = propertyLoader.getProperties();
        cxt.setProperties(pp);

        try {
            IOpenLBuilder builder = makeBuilder(openlName, cxt, ucxt);
            builder.setContexts(cxt, ucxt);

            return builder;
        } catch (Exception ex) {
            throw new OpenConfigurationException("Error creating builder: ", null, ex);
        }
    }

    public String makeAlternativeHome(String userHome) {
        try {
            return new File(userHome + "/../" + OPENL_ALT_CONFIG_ROOT).getCanonicalPath();
        } catch (Throwable t) {
            return null;
        }
    }

    private IOpenLBuilder makeBuilder(String openl, IConfigurableResourceContext cxt, IUserContext ucxt) throws Exception {

        String builderClassName = cxt.findProperty(openl + BUILDER_CLASS);
        if (builderClassName == null) {
            builderClassName = cxt.findProperty(DEFAULT_BUILDER_CLASS_PROPERTY);
        }

        String builderClassPath = cxt.findProperty(openl + BUILDER_CLASS_PATH);
        if (builderClassPath == null) {
            builderClassPath = cxt.findProperty(DEFAULT_BUILDER_CLASS_PATH_PROPERTY);
        }

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
    }

    IOpenLBuilder makeBuilderInstance(String builderClassName, String builderClassPath, IUserContext ucxt)
            throws Exception {

        ClassLoader cl = ClassLoaderFactory.getOpenlCoreClassLoader(ucxt.getUserClassLoader());
        if (builderClassPath != null) {
            cl = ClassLoaderFactory.createClassLoader(builderClassPath, cl, ucxt);
        }

        return (IOpenLBuilder) ClassFactory.newInstanceForName(builderClassName, cl);
    }
}
