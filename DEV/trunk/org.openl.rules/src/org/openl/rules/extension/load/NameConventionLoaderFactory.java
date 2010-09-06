package org.openl.rules.extension.load;

import org.apache.commons.lang.StringUtils;
import org.openl.exception.OpenLRuntimeException;

public class NameConventionLoaderFactory implements ILoaderFactory {

    public static ILoaderFactory INSTANCE = new NameConventionLoaderFactory();

    public IExtensionLoader getLoader(String name) {
        String className = "org.openl.rules." + name + "." + StringUtils.capitalize(name) + "Loader";
        try {
            return (IExtensionLoader) Class.forName(className).newInstance();
        } catch (Exception e) {
            throw new OpenLRuntimeException("Can't create loader: " + className);
        }
    }

}
