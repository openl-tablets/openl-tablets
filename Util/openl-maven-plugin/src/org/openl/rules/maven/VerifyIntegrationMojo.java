package org.openl.rules.maven;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.openl.rules.ruleservice.management.ServiceManagerImpl;
import org.openl.rules.ruleservice.servlet.MethodDescriptor;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Verifies if resulted archive is compatible with Rules Engine
 *
 * @author Vladyslav Pikus
 */
@Mojo(name = "verify", defaultPhase = LifecyclePhase.VERIFY)
public class VerifyIntegrationMojo extends BaseOpenLMojo {

    @Override
    void execute(String sourcePath, boolean hasDependencies) throws MojoFailureException {
        String pathDeployment = project.getAttachedArtifacts()
            .stream()
            .filter(artifact -> PackageMojo.DEPLOYMENT_CLASSIFIER.equals(artifact.getClassifier()))
            .findFirst()
            .orElseGet(project::getArtifact)
            .getFile()
            .getPath();

        final StandardEnvironment environment = new StandardEnvironment();
        Map<String, Object> props = new HashMap<>();
        props.put("production-repository.factory", "repo-zip");
        props.put("production-repository.archives", pathDeployment);
        environment.getPropertySources().addLast(new MapPropertySource("mavenIntegrationProperties", props));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setEnvironment(environment);
            context.register(Config.class);
            context.refresh();

            final RulesFrontend frontend = context.getBean(RulesFrontend.class);
            Collection<String> deployedServices = frontend.getServiceNames();
            if (deployedServices.size() == 0) {
                throw new MojoFailureException(
                    String.format("Failed to deploy '%s:%s'", project.getGroupId(), project.getArtifactId()));
            }
            final ServiceManagerImpl serviceManager = context.getBean("serviceManager", ServiceManagerImpl.class);
            for (String deployedService : deployedServices) {
                Collection<MethodDescriptor> methods = serviceManager.getServiceMethods(deployedService);
                if (methods == null || methods.size() == 0) {
                    throw new MojoFailureException(
                        String.format("OpenL Project '%s' has no public methods!", deployedService));
                }
            }
        }
        info(String.format("Verification is passed for '%s:%s' artifact", project.getGroupId(), project.getArtifactId()));
    }

    @ImportResource(locations = { "classpath:openl-ruleservice-beans.xml" })
    public static class Config {
    }
}
