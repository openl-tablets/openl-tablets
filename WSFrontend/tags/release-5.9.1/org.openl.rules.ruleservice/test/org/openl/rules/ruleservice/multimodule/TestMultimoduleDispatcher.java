package org.openl.rules.ruleservice.multimodule;

import java.util.Collection;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.publish.cache.dispatcher.ModuleDispatcherForMethods;

public class TestMultimoduleDispatcher implements ModuleDispatcherForMethods {

    public Module findModuleByName(Collection<Module> modules, String name) {
        for (Module module : modules) {
            if (module.getName().equals(name)) {
                return module;
            }
        }
        throw new OpenlNotCheckedException("Module not found");
    }

    @Override
    public Module getResponsibleModule(Collection<Module> modules,
            String methodName,
            Class<?>[] parameterTypes,
            IRulesRuntimeContext context) {
        return findModuleByName(modules, context.getUsState().name() + "_" + context.getLob());
    }
}
