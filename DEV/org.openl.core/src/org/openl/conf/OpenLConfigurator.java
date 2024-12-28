package org.openl.conf;

import org.openl.OpenL;

/**
 * This class is responsible for configuration of a particular OpenL instance designated by it's instance_name.
 *
 * @author snshor
 */

public class OpenLConfigurator {


    public static final String OPENL_BUILDER = "OpenLBuilder";

    public synchronized IOpenLBuilder getBuilder(String openlName, IUserContext ucxt) {
        String userHome = ucxt.getUserHome();

        var homes = new String[]{userHome};

        ConfigurableResourceContext cxt = new ConfigurableResourceContext(ucxt.getUserClassLoader(), homes);

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

        var builderClassName = openl + "." + OPENL_BUILDER;
        return (IOpenLBuilder) ClassFactory.forName(builderClassName, cl).getDeclaredConstructor().newInstance();
    }
}
