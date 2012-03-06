package org.openl.rules.ruleservice.multimodule;

import java.util.List;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.publish.cache.dispatcher.IModuleDispatcherForMethods;

public class TestMultimoduleDispatcher implements IModuleDispatcherForMethods {

    public Module findModuleByName(List<Module> modules, String name) {
        for (Module module : modules) {
            if (module.getName().equals(name)) {
                return module;
            }
        }
        throw new OpenlNotCheckedException("Module not found");
    }

    @Override
    public Module getResponsibleModule(List<Module> modules,
            String methodName,
            Class<?>[] parameterTypes,
            IRulesRuntimeContext context) {
        return findModuleByName(modules, context.getUsState().name() + "_" + context.getLob());
    }
}
