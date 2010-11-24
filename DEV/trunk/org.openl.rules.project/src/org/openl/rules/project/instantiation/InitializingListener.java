package org.openl.rules.project.instantiation;

import org.openl.rules.project.model.Module;

public interface InitializingListener {

    void afterModuleLoad(Module module);
}
