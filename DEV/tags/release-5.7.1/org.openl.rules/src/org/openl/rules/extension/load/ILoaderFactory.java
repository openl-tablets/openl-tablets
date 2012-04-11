package org.openl.rules.extension.load;

public interface ILoaderFactory {
    
    IExtensionLoader getLoader(String name);

}
