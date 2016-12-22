package org.openl.rules.ruleservice.loader;

import java.io.Closeable;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PreDestroy;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.deploy.DeployUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JCR repository data source. Uses
 * ProductionRepositoryFactoryProxy.getRepositoryInstance() repository. Thread
 * safe implementation.
 *
 * @author Marat Kamalov
 */
public class JcrDataSource implements DataSource {
    private final Logger log = LoggerFactory.getLogger(JcrDataSource.class);

    private Repository repository;

    /**
     * {@inheritDoc}
     */
    public Collection<Deployment> getDeployments() {
        return DeployUtils.getDeployments(repository);
    }

    /**
     * {@inheritDoc}
     */
    public Deployment getDeployment(String deploymentName, CommonVersion deploymentVersion) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName argument can't be null");
        }
        if (deploymentVersion == null) {
            throw new IllegalArgumentException("deploymentVersion argument can't be null");
        }

        log.debug("Getting deployement with name=\"{}\" and version=\"{}\"",
            deploymentName,
            deploymentVersion.getVersionName());

        String name = deploymentName + DeployUtils.SEPARATOR + deploymentVersion.getVersionName();
        return new Deployment(repository, DeployUtils.DEPLOY_PATH + name, deploymentName, deploymentVersion, false);
    }

    /**
     * {@inheritDoc}
     */
    public void setListener(DataSourceListener dataSourceListener) {
        if (dataSourceListener == null) {
            repository.setListener(null);
        } else {
            repository.setListener(new DataSourceListenerWrapper(dataSourceListener));
        }
    }

    /**
     * For Spring framework
     */
    @PreDestroy
    public void destroy() throws Exception {
        log.debug("JCR data source releasing");
        if (repository instanceof Closeable) {
            ((Closeable) repository).close();
        }
    }

    public void setRepository(Repository repository) throws RRepositoryException {
        this.repository = repository;
    }

    private static class DataSourceListenerWrapper implements Listener {
        private final Logger log = LoggerFactory.getLogger(DataSourceListenerWrapper.class);
        private final DataSourceListener dataSourceListener;

        public DataSourceListenerWrapper(DataSourceListener dataSourceListeners) {
            this.dataSourceListener = dataSourceListeners;
        }

        @Override
        public synchronized void onChange() {
            final Timer timer = new Timer();

            timer.schedule(new TimerTask() {
                int count = 0;

                @Override
                public void run() {
                    try {
                        log.info("Atempt to deploy # {}", count);
                        System.gc();
                        dataSourceListener.onDeploymentAdded();
                        timer.cancel();
                    } catch (Exception ex) {
                        log.error("Unexpected error", ex);
                        count++;
                        if (count >= 5) {
                            timer.cancel();
                        }
                    }
                }
            }, 1000, 3000);
        }
    }
}
