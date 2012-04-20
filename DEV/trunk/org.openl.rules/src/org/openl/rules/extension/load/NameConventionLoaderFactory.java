package org.openl.rules.extension.load;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NameConventionLoaderFactory implements ILoaderFactory {
    private final Log log = LogFactory.getLog(NameConventionLoaderFactory.class);

    public static ILoaderFactory INSTANCE = new NameConventionLoaderFactory();

    public IExtensionLoader getLoader(String name) {
        String className = "org.openl.rules." + name + "." + StringUtils.capitalize(name) + "Loader";
        try {
            return (IExtensionLoader) Class.forName(className).newInstance();
        } catch (Exception e) {
            log.warn(String.format("Can't create loader: %s", className));
        }
        return null;
    }

}
