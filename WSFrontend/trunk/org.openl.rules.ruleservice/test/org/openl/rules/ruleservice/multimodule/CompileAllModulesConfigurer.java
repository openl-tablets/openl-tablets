package org.openl.rules.ruleservice.multimodule;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.ModuleDescription;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.rules.ruleservice.management.ServiceConfigurer;
import org.openl.rules.ruleservice.publish.cache.dispatcher.DispatchedMethod;

public class CompileAllModulesConfigurer implements ServiceConfigurer {
    public static interface TestServiceClass {
        @DispatchedMethod(dispatcher = TestMultimoduleDispatcher.class)
        String someMethod(IRulesRuntimeContext context);
    }

    @Override
    public List<ServiceDescription> getServicesToBeDeployed(RuleServiceLoader loader) {
        Set<ModuleDescription> modules = new HashSet<ModuleDescription>();
        for (Deployment deployment : loader.getDeployments()) {
            for (AProject project : deployment.getProjects()) {
                for (Module module : loader.resolveModulesForProject(deployment.getDeploymentName(),
                    deployment.getCommonVersion(),
                    project.getName())) {
                    modules.add(new ModuleDescription.ModuleDescriptionBuilder().setDeploymentName(deployment.getDeploymentName())
                        .setDeploymentVersion(deployment.getCommonVersion())
                        .setProjectName(project.getName())
                        .setModuleName(module.getName())
                        .build());
                }
            }
        }
        return Collections.singletonList(new ServiceDescription.ServiceDescriptionBuilder().setName("service")
            .setUrl("service")
            .setServiceClassName(TestServiceClass.class.getName())
            .setProvideRuntimeContext(true)
            .setModules(modules)
            .build());
    }
}
