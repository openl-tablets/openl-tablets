package org.openl.rules.ruleservice.loader;

import java.util.*;

import javax.annotation.PreDestroy;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.api.FileData;
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

    private static final String SEPARATOR = "#";

    final List<DataSourceListener> listeners = new ArrayList<DataSourceListener>();
    private DataSourceListenerWrapper wrapper = new DataSourceListenerWrapper(listeners);

    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;
    private String repositoryPropertiesFile = ProductionRepositoryFactoryProxy.DEFAULT_REPOSITORY_PROPERTIES_FILE; // For
    // backward
    // compatibility

    /**
     * {@inheritDoc}
     */
    public Collection<Deployment> getDeployments() {
        Repository repository = getRProductionRepository();

        Collection<FileData> fileDatas = repository.list(DeployUtils.DEPLOY_PATH);
        Collection<Deployment> ret = new ArrayList<Deployment>();
        for (FileData fileData : fileDatas) {
            ret.add(new Deployment(repository, fileData));
        }
        return ret;
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

        String name = deploymentName + SEPARATOR + deploymentVersion.getVersionName();
        // FIXME
        // Should be deploymentNotFoundException or null return
        Repository repository = getRProductionRepository();
        return new Deployment(repository, DeployUtils.DEPLOY_PATH + name, deploymentName, deploymentVersion);
    }

    private Repository getRProductionRepository() {
        try {
            return productionRepositoryFactoryProxy.getRepositoryInstance(repositoryPropertiesFile);
        } catch (RRepositoryException e) {
            throw new DataSourceException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addListener(DataSourceListener dataSourceListener) {
        if (dataSourceListener == null) {
            throw new IllegalArgumentException("dataSourceListener argument can't be null");
        }
        synchronized (listeners) {
            if (!listeners.contains(dataSourceListener)) {
                listeners.add(dataSourceListener);
                if (listeners.size() == 1) {
                    getRProductionRepository().setListener(wrapper);
                }
                log.info("{} listener is registered in jcr data source", dataSourceListener.getClass());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeListener(DataSourceListener dataSourceListener) {
        if (dataSourceListener == null) {
            throw new IllegalArgumentException("dataSourceListener argument can't be null");
        }
        synchronized (listeners) {
            listeners.remove(dataSourceListener);
            if (listeners.isEmpty()) {
                getRProductionRepository().setListener(null);
            }
        }
    }

    /**
     * For Spring framework
     */
    @PreDestroy
    public void destroy() throws Exception {
        log.debug("JCR data source releasing");
        productionRepositoryFactoryProxy.releaseRepository(repositoryPropertiesFile);
    }

    public void setProductionRepositoryFactoryProxy(ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy) {
        this.productionRepositoryFactoryProxy = productionRepositoryFactoryProxy;
    }

    public void setRepositoryPropertiesFile(String repositoryPropertiesFile) {
        this.repositoryPropertiesFile = repositoryPropertiesFile;
    }

    private static class DataSourceListenerWrapper implements Listener {
        private final Logger log = LoggerFactory.getLogger(DataSourceListenerWrapper.class);
        private final List<DataSourceListener> dataSourceListeners;

        public DataSourceListenerWrapper(List<DataSourceListener> dataSourceListeners) {
            if (dataSourceListeners == null) {
                throw new IllegalArgumentException();
            }
            this.dataSourceListeners = dataSourceListeners;
        }

        @Override
        public synchronized void onChange() {
            for (final DataSourceListener dataSourceListener : dataSourceListeners) {
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
}
