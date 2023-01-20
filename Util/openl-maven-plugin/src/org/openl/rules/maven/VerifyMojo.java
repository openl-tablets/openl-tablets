package org.openl.rules.maven;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.management.ServiceManagerImpl;
import org.openl.rules.ruleservice.servlet.SpringInitializer;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Verifies if resulted archive is compatible with the OpenL Tablets Rules Engine
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

    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    private File outputDirectory;

    @Parameter( defaultValue = "${plugin}", readonly = true )
    private PluginDescriptor plugin;

    @Override
    void execute(String sourcePath, boolean hasDependencies) throws MojoFailureException, IOException {
        String pathDeployment = project.getAttachedArtifacts()
            .stream()
            .filter(artifact -> PackageMojo.DEPLOYMENT_CLASSIFIER.equals(artifact.getClassifier()))
            .findFirst()
            .orElseGet(project::getArtifact)
            .getFile()
            .getPath();

        final ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        final var pluginClassRealm = plugin.getClassRealm();
        final var newClassloader = new ClassRealm(pluginClassRealm.getWorld(), "verify", oldClassloader);
        newClassloader.setParentRealm(pluginClassRealm);
        for (File f : getTransitiveDependencies()) {
            newClassloader.addURL(f.toURI().toURL());
        }

        try {
            Thread.currentThread().setContextClassLoader(newClassloader);
            // Without it Embedded Tomcat may fail to start while build.
            TomcatURLStreamHandlerFactory.disable();
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SpringBootWebApp.class)
                .properties(Map.of(
                    "server.port", 0,
                    "server.servlet.context-parameters.openl.config.location", "",
                    "server.servlet.context-parameters.user.home", outputDirectory.getPath(),
                    "server.servlet.context-parameters.production-repository.factory", "repo-zip",
                    "server.servlet.context-parameters.production-repository.archives", pathDeployment
                )).run()) {
                ApplicationContext openLContext = SpringInitializer
                    .getApplicationContext(context.getBean(ServletContext.class));

                final ServiceManagerImpl serviceManager = openLContext.getBean("serviceManager",
                    ServiceManagerImpl.class);
                Collection<OpenLService> deployedServices = serviceManager.getServices();
                if (deployedServices.isEmpty()) {
                    throw new MojoFailureException(
                        String.format("Failed to deploy '%s:%s'.", project.getGroupId(), project.getArtifactId()));
                }
                for (OpenLService service : deployedServices) {
                    if (!serviceManager.getServiceErrors(service.getDeployPath()).isEmpty()) {
                        Throwable rootError = service.getException();
                        if (isNoPublicMethodError(rootError)) {
                            throw new MojoFailureException(
                                String.format("The deployment '%s' has no public methods.", service.getDeployPath()),
                                rootError);
                        } else {
                            throw new MojoFailureException(
                                String.format("OpenL Project '%s' has errors!", service.getDeployPath()),
                                rootError);
                        }
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
        info(String
            .format("Verification is passed for '%s:%s' artifact.", project.getGroupId(), project.getArtifactId()));
    }

    private static boolean isNoPublicMethodError(Throwable error) {
        return error instanceof ServiceConstructionException && "No resource classes found".equals(error.getMessage());
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

    @SpringBootApplication
    @ServletComponentScan("org.openl.rules.ruleservice.servlet")
    static class SpringBootWebApp {

    }

}
