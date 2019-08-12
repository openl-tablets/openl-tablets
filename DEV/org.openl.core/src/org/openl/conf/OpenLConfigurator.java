package org.openl.conf;

import java.io.File;
import java.util.Properties;

import org.openl.OpenL;

/**
 *
 * This class is responsible for configuration of a particular OpenL instance designated by it's instance_name.
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

    public static final String OPENL_BUILDER = "OpenLBuilder";

    public synchronized IOpenLBuilder getBuilder(String openlName, IUserContext ucxt) {
        String userHome = ucxt.getUserHome();

        String[] homes;
        try {
            String altHome = new File(userHome + "/../" + OPENL_ALT_CONFIG_ROOT).getCanonicalPath();
            homes = new String[] { userHome, altHome };
        } catch (Exception t) {
            homes = new String[] { userHome };
        }

        ConfigurableResourceContext cxt = new ConfigurableResourceContext(ucxt.getUserClassLoader(), homes);

        PropertyFileLoader propertyLoader = new PropertyFileLoader(openlName + "." + OPENL_DEFAULT_PROPERTY_FILE_NAME,
            openlName + "." + OPENL_PROPERTY_FILE_PROPERTY,
            cxt,
            new PropertyFileLoader(OPENL_DEFAULT_PROPERTY_FILE_NAME,
                "org.openl." + OPENL_PROPERTY_FILE_PROPERTY,
                cxt,
                null));

        Properties pp = propertyLoader.getProperties();
        cxt.setProperties(pp);

        try {
            IOpenLBuilder builder = makeBuilder(openlName, cxt, ucxt);
            builder.setContexts(cxt, ucxt);

            return builder;
        } catch (Exception ex) {
            throw new OpenLConfigurationException("Error creating builder: ", null, ex);
        }
    }

    private IOpenLBuilder makeBuilder(String openl,
            IConfigurableResourceContext cxt,
            IUserContext ucxt) throws Exception {
        ClassLoader ucl = ucxt.getUserClassLoader();
        ClassLoader cl = OpenL.class.getClassLoader();
        try {
            Class<?> c = ucl.loadClass(OpenL.class.getName());
            if (c != null) {
                cl = ucl;
            }
        } catch (Exception ignored) {
            // Ignore
        }

        String builderClassName = cxt.findProperty(openl + BUILDER_CLASS);
        if (builderClassName == null) {
            builderClassName = cxt.findProperty(DEFAULT_BUILDER_CLASS_PROPERTY);
        }
        if (builderClassName == null) {
            builderClassName = openl + "." + OPENL_BUILDER;
        }
        return (IOpenLBuilder) ClassFactory.forName(builderClassName, cl).newInstance();
    }
}
