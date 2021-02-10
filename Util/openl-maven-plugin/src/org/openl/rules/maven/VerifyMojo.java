package org.openl.rules.maven;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.management.ServiceManagerImpl;
import org.openl.rules.ruleservice.servlet.MethodDescriptor;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Verifies if resulted archive is compatible with Rules Engine
 *
 * @author Vladyslav Pikus
 * @since 5.24.0
 */
@Mojo(name = "verify", defaultPhase = LifecyclePhase.VERIFY)
public class VerifyMojo extends BaseOpenLMojo {

    /**
     * Parameter to skip running OpenL Tablets verify goal if it set to 'true'.
     */
    @Parameter(property = "skipTests")
    private boolean skipTests;

    /**
     * Parameter to skip running OpenL Tablets verify goal if it set to 'true'.
     * 
     * @deprecated for troubleshooting purposes
     */
    @Parameter(property = "skipITs")
    @Deprecated
    private boolean skipITs;

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

        try (GenericXmlApplicationContext context = new GenericXmlApplicationContext()) {
            context.setEnvironment(environment);
            context.load("classpath:openl-ruleservice-beans.xml");
            context.refresh();

            final RulesFrontend frontend = context.getBean(RulesFrontend.class);

            Collection<String> deployedServices = frontend.getServices().stream().map(OpenLService::getDeployPath).collect(Collectors.toList());
            if (deployedServices.isEmpty()) {
                throw new MojoFailureException(
                    String.format("Failed to deploy '%s:%s'.", project.getGroupId(), project.getArtifactId()));
            }
            final ServiceManagerImpl serviceManager = context.getBean("serviceManager", ServiceManagerImpl.class);
            boolean hasMethods = false;
            for (String deployedService : deployedServices) {
                OpenLService service = serviceManager.getServiceByDeploy(deployedService);
                try {
                    // trigger service class instantiation
                    service.getServiceClass();
                } catch (RuleServiceInstantiationException e) {
                    throw new MojoFailureException(String.format("OpenL Project '%s' has errors!", deployedService), e);
                }
                if (!serviceManager.getServiceErrors(deployedService).isEmpty()) {
                    throw new MojoFailureException(String.format("OpenL Project '%s' has errors!", deployedService));
                }
                Collection<MethodDescriptor> methods = serviceManager.getServiceMethods(deployedService);
                hasMethods |= methods != null && !methods.isEmpty();
            }
            if (!hasMethods) {
                throw new MojoFailureException(String.format("The deployment '%s:%s' has no public methods.",
                    project.getGroupId(),
                    project.getArtifactId()));
            }
        }
        info(String
            .format("Verification is passed for '%s:%s' artifact", project.getGroupId(), project.getArtifactId()));
    }

    @Override
    boolean isDisabled() {
        return skipTests || skipITs;
    }

    @Override
    String getHeader() {
        return "OPENL VERIFY";
    }
}
