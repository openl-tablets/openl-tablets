package org.openl.rules.extension.load;

/**
 * @deprecated Will be deleted soon. Now extension is declared in rules.xml.
 */
@Deprecated
public interface ILoaderFactory {
    
    IExtensionLoader getLoader(String name);

}
