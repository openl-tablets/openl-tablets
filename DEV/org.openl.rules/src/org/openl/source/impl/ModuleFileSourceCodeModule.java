package org.openl.source.impl;

import java.net.URL;

import lombok.Getter;

import org.openl.types.IModuleInfo;

/**
 * @deprecated use {@link URLSourceCodeModule}
 */
// A source is equal by its URL; the module name only labels it.
@SuppressWarnings("java:S2160")
@Deprecated(since = "5.23.10")
public class ModuleFileSourceCodeModule extends URLSourceCodeModule implements IModuleInfo {
    @Getter
    private final String moduleName;

    public ModuleFileSourceCodeModule(URL url, String moduleName) {
        super(url);
        this.moduleName = moduleName;
    }
}
