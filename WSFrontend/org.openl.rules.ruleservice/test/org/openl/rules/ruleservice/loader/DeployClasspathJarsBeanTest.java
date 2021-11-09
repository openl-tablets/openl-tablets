package org.openl.rules.ruleservice.loader;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;

public class DeployClasspathJarsBeanTest {
    @Test
    public void afterPropertiesSet() throws Exception {
        DeployClasspathJarsBean classpathDeployer = new DeployClasspathJarsBean();
        classpathDeployer.setEnabled(true);
        classpathDeployer.setRetryPeriod(1);
        classpathDeployer.setFilesToDeploy(Arrays.asList(new File("1"), new File("2")));

        // Iterations:
        // 1. Deployer service is unavailable
        // 2. Deployer service is available.
        // 2.1 Deploy of first file is successful.
        // 2.2 Deploy of second file is unsuccessful (throws IOException).
        // 3. Deployer service is unavailable.
        // 4. Deployer service is available.
        // 4.1 Deploy of last file is successful.
        AtomicBoolean available = new AtomicBoolean(false);
        RulesDeployerService unstableDeployerService = mock(RulesDeployerService.class);
        when(unstableDeployerService.isReady()).thenAnswer(invocation -> {
            final boolean result = available.get();
            if (!result) {
                available.set(true);
            }
            return result;
        });
        doAnswer(invocation -> {
            if (available.get()) {
                available.set(false);
                return null;
            }
            throw new IOException("Not available");
        }).when(unstableDeployerService).deploy(any(File.class), anyBoolean());
        classpathDeployer.setRulesDeployerService(unstableDeployerService);

        // Finalize deployer initialization and start deploy process.
        classpathDeployer.afterPropertiesSet();

        // Waiting until the deployer will finish its work.
        TimeUnit.SECONDS.sleep(4);

        verify(unstableDeployerService, times(4)).isReady();
        verify(unstableDeployerService, times(3)).deploy(any(File.class), anyBoolean());
    }
}