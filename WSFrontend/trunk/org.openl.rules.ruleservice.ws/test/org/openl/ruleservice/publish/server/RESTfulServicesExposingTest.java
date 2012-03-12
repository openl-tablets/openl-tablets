package org.openl.ruleservice.publish.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.ModuleDescription;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.rules.ruleservice.management.ServiceConfigurer;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RESTfulServicesExposingTest {
    private static final String TUTORIAL4_SERVICE_URL = "org.openl.tablets.tutorial4";

    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "classpath:RESTful/openl-ruleservice-beans.xml");
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        serviceManager.start();
        System.out.print("Press enter for server stop:");
        System.in.read();
        System.out.println("Server stoped");
        System.exit(0);
    }

    public static class TestConfigurer implements ServiceConfigurer {

        private ServiceDescription resolveTutorial4Service(RuleServiceLoader loader) {
            Collection<Deployment> deployments = loader.getDeployments();
            assert (deployments.size() == 1);
            Deployment deployment = deployments.iterator().next();
            
            Set<ModuleDescription> moduleConfigurations = new HashSet<ModuleDescription>();
            
            for (AProject project : deployment.getProjects()) {
                Collection<Module> modulesOfProject = loader.resolveModulesForProject(deployment.getDeploymentName(),
                        deployment.getCommonVersion(), project.getName());
                for (Module module : modulesOfProject) {
                    moduleConfigurations.add(
                            new ModuleDescription.ModuleDescriptionBuilder()
                                .setDeploymentName(deployment.getDeploymentName())
                                .setDeploymentVersion(deployment.getCommonVersion())
                                .setProjectName(project.getName())
                                .setModuleName(module.getName())
                                .build()
                    );
                }
            }
            
            assert (!moduleConfigurations.isEmpty());
            
            return new ServiceDescription.ServiceDescriptionBuilder()
                .setName("tutorial4")
                .setUrl(TUTORIAL4_SERVICE_URL)
                .setServiceClassName("org.openl.rules.tutorial4.Tutorial4Interface")
                .setProvideRuntimeContext(false)
                .setModules(moduleConfigurations)
                .build();
        }

        @Override
        public List<ServiceDescription> getServicesToBeDeployed(RuleServiceLoader loader) {
            List<ServiceDescription> services = new ArrayList<ServiceDescription>();
            services.add(resolveTutorial4Service(loader));
            return services;
        }

    }
}
