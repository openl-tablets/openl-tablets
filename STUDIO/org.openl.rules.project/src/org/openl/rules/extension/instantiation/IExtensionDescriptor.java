package org.openl.rules.extension.instantiation;

import java.util.List;

import org.openl.rules.project.model.Module;
import org.openl.source.IOpenSourceCodeModule;

public interface IExtensionDescriptor {
    String getOpenLName();

    List<Module> getInternalModules(Module module);

    IOpenSourceCodeModule getSourceCode(Module module);

    String getUrlForModule(Module module);
}
