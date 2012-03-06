package org.openl.rules.ruleservice.publish.cache.dispatcher;

import java.util.List;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.model.Module;

/**
 * Determines the module containing required Data table from collection of
 * Modules that was passed to current service.
 * 
 * @author PUdalau
 */
public interface IModuleDispatcherForData {
    Module getResponsibleModule(List<Module> modules, String fieldName, IRulesRuntimeContext context);
}
