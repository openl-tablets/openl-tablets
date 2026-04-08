package org.openl.rules.ruleservice.loader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import org.openl.rules.ruleservice.deployer.RulesDeployerService;

public class DeployClasspathJarsBeanTest {
    @Test
    public void test() throws Exception {
        var unstableDeployerService = mock(RulesDeployerService.class);
        var propertyResolver = mock(PropertyResolver.class);
        when(propertyResolver.getProperty("production-repository.factory")).thenReturn("repo-jdbc");
        var classpathDeployer = new DeployClasspathJarsBean(unstableDeployerService,
                "ALWAYS",
                0);
        classpathDeployer.resourceResolver = mock(PathMatchingResourcePatternResolver.class);
        var resource = mock(Resource.class);
        when(resource.getURL()).thenReturn(new File(".").toURI().toURL());
        when(classpathDeployer.resourceResolver.getResources(any(String.class))).thenReturn(new Resource[0], new Resource[0], new Resource[0], new Resource[]{resource, resource});

        // Iterations:
        // 1. Deployer service is unavailable
        // 2. Deployer service is available.
        // 2.1 Deploy of first file is successful.
        // 2.2 Deploy of second file is unsuccessful (throws IOException).
        // 3. Deployer service is unavailable.
        // 4. Deployer service is available.
        // 4.1 Deploy of last file is successful.
        var done = new CountDownLatch(3);
        AtomicBoolean available = new AtomicBoolean(false);
        when(unstableDeployerService.isReady()).thenAnswer(invocation -> {
            final boolean result = available.get();
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
        }).when(unstableDeployerService).deploy(any(File.class), anyBoolean());

        // Finalize deployer initialization.
        classpathDeployer.start();

        // Wait until the deployer finishes all deploy attempts.
        done.await(10, TimeUnit.SECONDS);

        verify(unstableDeployerService, times(4)).isReady();
        verify(unstableDeployerService, times(3)).deploy(any(File.class), anyBoolean());
    }
}
