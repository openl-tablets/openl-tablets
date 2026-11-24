package org.openl.rules.ruleservice.loader;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import org.openl.rules.repository.zip.ClasspathArchives;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;

class DeployClasspathJarsBeanTest {

    @Test
    void startsWithTheApplicationAlthoughItsConfigurationIsLazy() {
        var deployerService = mock(RulesDeployerService.class);
        try (var context = new GenericXmlApplicationContext()) {
            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource("test",
                            Map.of("production-repository.factory", "repo-file",
                                    "ruleservice.datasource.deploy.classpath.jars", "IF_ABSENT",
                                    "ruleservice.datasource.deploy.classpath.retry-period", "0")));
            context.load("classpath:openl-ruleservice-datasource-beans.xml");
            context.getBeanFactory().registerSingleton("rulesDeployerService", deployerService);
            context.refresh();

            // Nothing asks for the bean: the context refresh alone must create it, which starts the deployment.
            assertTrue(context.getBeanFactory().containsSingleton("deployClasspathJarsBean"));
        }
    }

    @Test
    void test() throws Exception {
        var unstableDeployerService = mock(RulesDeployerService.class);
        var propertyResolver = mock(PropertyResolver.class);
        when(propertyResolver.getProperty("production-repository.factory")).thenReturn("repo-jdbc");
        var classpathDeployer = new DeployClasspathJarsBean(unstableDeployerService,
                DeployStrategy.ALWAYS,
                0);
        var resourceResolver = mock(PathMatchingResourcePatternResolver.class);
        classpathDeployer.archives = new ClasspathArchives(resourceResolver);
        var first = mock(Resource.class);
        when(first.getURI()).thenReturn(new File("first.zip").toURI());
        var second = mock(Resource.class);
        when(second.getURI()).thenReturn(new File("second.zip").toURI());
        // The same archive found twice is deployed once
        when(resourceResolver.getResources(any(String.class))).thenReturn(new Resource[0],
                new Resource[0],
                new Resource[0],
                new Resource[]{first, second, first});

        // Iterations:
        // 1. Deployer service is unavailable
        // 2. Deployer service is available.
        // 2.1 Deploy of first file is successful.
        // 2.2 Deploy of second file is unsuccessful (throws IOException).
        // 3. Deployer service is unavailable.
        // 4. Deployer service is available.
        // 4.1 Deploy of last file is successful.
        var done = new CountDownLatch(3);
        var available = new AtomicBoolean(false);
        when(unstableDeployerService.isReady()).thenAnswer(invocation -> {
            final var result = available.get();
            if (!result) {
                available.set(true);
            }
            return result;
        });
        doAnswer(invocation -> {
            done.countDown();
            if (available.get()) {
                available.set(false);
                return null;
            }
            throw new IOException("Not available");
        }).when(unstableDeployerService).deploy(any(Path.class), anyBoolean());

        // Finalize deployer initialization.
        classpathDeployer.start();

        // Wait until the deployer finishes all deploy attempts.
        done.await(10, TimeUnit.SECONDS);

        verify(unstableDeployerService, times(4)).isReady();
        verify(unstableDeployerService, times(3)).deploy(any(Path.class), anyBoolean());
    }

    @Test
    void invalidArchiveIsNotRetried() throws Exception {
        var deployerService = mock(RulesDeployerService.class);
        when(deployerService.isReady()).thenReturn(true);
        var invalid = Path.of("empty.zip").toAbsolutePath();
        var valid = Path.of("rules.zip").toAbsolutePath();
        doThrow(new IllegalArgumentException("Zip file is empty.")).when(deployerService).deploy(invalid, false);
        var deployed = new CountDownLatch(1);
        doAnswer(invocation -> {
            deployed.countDown();
            return null;
        }).when(deployerService).deploy(valid, false);
        var invalidResource = mock(Resource.class);
        when(invalidResource.getURI()).thenReturn(invalid.toUri());
        var validResource = mock(Resource.class);
        when(validResource.getURI()).thenReturn(valid.toUri());
        var resourceResolver = mock(PathMatchingResourcePatternResolver.class);
        when(resourceResolver.getResources(any(String.class))).thenReturn(new Resource[0],
                new Resource[0],
                new Resource[0],
                new Resource[]{invalidResource, validResource});
        var classpathDeployer = new DeployClasspathJarsBean(deployerService, DeployStrategy.IF_ABSENT, 60);
        classpathDeployer.archives = new ClasspathArchives(resourceResolver);

        classpathDeployer.start();

        assertTrue(deployed.await(10, TimeUnit.SECONDS), "The archive behind the invalid one is deployed");
        verify(deployerService, times(1)).deploy(invalid, false);
        classpathDeployer.destroy();
    }
}
