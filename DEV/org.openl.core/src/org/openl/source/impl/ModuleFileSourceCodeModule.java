package org.openl.source.impl;

import java.net.URL;

import org.openl.types.IModuleInfo;

/**
 * @deprecated use {@link URLSourceCodeModule}
 */
@Deprecated
public class ModuleFileSourceCodeModule extends URLSourceCodeModule implements IModuleInfo {
    private final String moduleName;

    public ModuleFileSourceCodeModule(URL url, String moduleName) {
        super(url);
        this.moduleName = moduleName;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }
}
