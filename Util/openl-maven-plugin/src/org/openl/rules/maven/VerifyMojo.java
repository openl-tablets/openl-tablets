package org.openl.rules.maven;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
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
@Mojo(name = "verify", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
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
    void execute(String sourcePath, boolean hasDependencies) throws MojoFailureException, MalformedURLException {
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

        List<URL> transitiveDeps = new ArrayList<>();
        for (File f : getTransitiveDependencies()) {
            transitiveDeps.add(f.toURI().toURL());
        }

        final ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        try {
            URLClassLoader newClassloader = new URLClassLoader(transitiveDeps.toArray(new URL[0]), oldClassloader);
            Thread.currentThread().setContextClassLoader(newClassloader);
            try (GenericXmlApplicationContext context = new GenericXmlApplicationContext()) {
                context.setEnvironment(environment);
                context.load("classpath:openl-ruleservice-beans.xml");
                context.refresh();

                final RulesFrontend frontend = context.getBean(RulesFrontend.class);
                Collection<String> deployedServices = frontend.getServices()
                    .stream()
                    .map(OpenLService::getDeployPath)
                    .collect(Collectors.toList());
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
                        throw new MojoFailureException(String.format("OpenL Project '%s' has errors!", deployedService),
                            e);
                    }
                    if (!serviceManager.getServiceErrors(deployedService).isEmpty()) {
                        throw new MojoFailureException(
                            String.format("OpenL Project '%s' has errors!", deployedService));
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
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
        info(String
            .format("Verification is passed for '%s:%s' artifact.", project.getGroupId(), project.getArtifactId()));
    }

    private Set<File> getTransitiveDependencies() {
        Set<String> allowedDependencies = getAllowedDependencies();
        return getDependentNonOpenLProjects().stream().filter(artifact -> {
            if (isOpenLCoreDependency(artifact.getGroupId())) {
                debug("SKIP : ", artifact);
                return false;
            }
            return true;
        }).filter(artifact -> {
            List<String> dependencyTrail = artifact.getDependencyTrail();
            if (dependencyTrail.size() < 2) {
                debug("SKIP : ", artifact, " (by dependency depth)");
                return false; // skip, unexpected size of dependencies
            }
            if (skipOpenLCoreDependency(dependencyTrail)) {
                debug("SKIP : ", artifact, " (transitive dependency from OpenL or SLF4j dependencies)");
                return false;
            }
            return true;
        }).filter(artifact -> {
            String tr = artifact.getDependencyTrail().get(1);
            String key = tr.substring(0, tr.indexOf(':', tr.indexOf(':') + 1));
            return allowedDependencies.contains(key);
        }).map(Artifact::getFile).collect(Collectors.toSet());
    }

    private Set<String> getAllowedDependencies() {
        return project.getDependencies().stream().filter(dep -> {
            if (skipToProcess(dep.getScope(), dep.getGroupId())) {
                debug("SKIP : ", dep);
                return false;
            }
            return true;
        }).map(dep -> ArtifactUtils.versionlessKey(dep.getGroupId(), dep.getArtifactId())).collect(Collectors.toSet());
    }

    private static boolean skipToProcess(String scope, String group) {
        return !Artifact.SCOPE_PROVIDED.equals(scope) || isOpenLCoreDependency(group);
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
