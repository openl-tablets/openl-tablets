package org.openl.rules.ruleservice.publish.cache.dispatcher;

import java.util.Collection;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.model.Module;

/**
 * Determines the module containing required Data table from collection of
 * Modules that was passed to current service.
 * 
 * Module selection is based on runtime context.
 * 
 * @author PUdalau
 */
public interface IModuleDispatcherForMethods {

    Module getResponsibleModule(Collection<Module> modules,
            String methodName,
            Class<?>[] parameterTypes,
            IRulesRuntimeContext context);
}
