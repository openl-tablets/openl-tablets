package org.openl.rules.extension.load;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated Will be deleted soon. Now extension is declared in rules.xml.
 */
@Deprecated
public class NameConventionLoaderFactory implements ILoaderFactory {
    private final Logger log = LoggerFactory.getLogger(NameConventionLoaderFactory.class);

    public static ILoaderFactory INSTANCE = new NameConventionLoaderFactory();

    public IExtensionLoader getLoader(String name) {
        String className = "org.openl.rules." + name + "." + StringUtils.capitalize(name) + "Loader";
        try {
            return (IExtensionLoader) Class.forName(className).newInstance();
        } catch (Exception e) {
            log.warn("Can't create loader: {}", className);
        }
        return null;
    }

}
