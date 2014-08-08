package org.openl.rules.ruleservice.loader;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.*;

/**
 * JCR repository data source. Uses
 * ProductionRepositoryFactoryProxy.getRepositoryInstance() repository. Thread
 * safe implementation.
 *
 * @author Marat Kamalov
 */
public class JcrDataSource implements DataSource, DisposableBean {
    private final Logger log = LoggerFactory.getLogger(JcrDataSource.class);

    private static final String SEPARATOR = "#";

    private Map<DataSourceListener, RDeploymentListener> listeners = new HashMap<DataSourceListener, RDeploymentListener>();

    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;
    private String repositoryPropertiesFile = ProductionRepositoryFactoryProxy.DEFAULT_REPOSITORY_PROPERTIES_FILE; // For
    // backward
    // compatibility
    private boolean shouldDestroyProxy = false;

    /**
     * {@inheritDoc}
     */
    public Collection<Deployment> getDeployments() {
        try {
            List<FolderAPI> deploymentProjects = getRProductionRepository().getDeploymentProjects();
            Collection<Deployment> ret = new ArrayList<Deployment>();
            for (FolderAPI deploymentProject : deploymentProjects) {
                String deploymentName = deploymentProject.getName();
                int separatorPosition = deploymentName.lastIndexOf(SEPARATOR);

                if (separatorPosition < 0) {
                    ret.add(new Deployment(deploymentProject));
                } else {
                    String name = deploymentName.substring(0, separatorPosition);
                    String version = deploymentName.substring(separatorPosition + 1);
                    CommonVersion commonVersion = new CommonVersionImpl(version);
                    ret.add(new Deployment(deploymentProject, name, commonVersion));
                }
            }
            return ret;
        } catch (RRepositoryException e) {
            throw new DataSourceException(e);
        }
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

        log.debug("Getting deployement with name=\"{}\" and version=\"{}\"", deploymentName, deploymentVersion.getVersionName());

        try {
            StringBuilder sb = new StringBuilder(deploymentName);
            sb.append(SEPARATOR);
            sb.append(deploymentVersion.getVersionName());
            // FIXME
            // Should be deploymentNotFoundException or null return
            FolderAPI deploymentProject = getRProductionRepository().getDeploymentProject(sb.toString());
            return new Deployment(deploymentProject, deploymentName, deploymentVersion);
        } catch (RRepositoryException e) {
            throw new DataSourceException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<DataSourceListener> getListeners() {
        List<DataSourceListener> tmp = null;
        synchronized (listeners) {
            Collection<DataSourceListener> dataSourceListeners = listeners.keySet();
            tmp = new ArrayList<DataSourceListener>(dataSourceListeners);
        }
        return Collections.unmodifiableList(tmp);
    }

    private RProductionRepository getRProductionRepository() {
        RProductionRepository rProductionRepository = null;
        try {
            rProductionRepository = getProductionRepositoryFactoryProxy().getRepositoryInstance(
                    getRepositoryPropertiesFile());
            return rProductionRepository;
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
            RDeploymentListener rDeploymentListener = listeners.get(dataSourceListener);
            if (rDeploymentListener == null) {
                rDeploymentListener = buildRDeploymentListener(dataSourceListener);
                try {
                    getRProductionRepository().addListener(rDeploymentListener);
                    listeners.put(dataSourceListener, rDeploymentListener);
                    log.info("{} listener is registered in jcr data source", dataSourceListener.getClass());
                } catch (RRepositoryException e) {
                    throw new DataSourceException(e);
                }
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
            RDeploymentListener listener = listeners.get(dataSourceListener);
            if (listener != null) {
                try {
                    getRProductionRepository().removeListener(listener);
                    listeners.remove(dataSourceListener);
                    log.info("{} listener is unregistered from jcr data source", dataSourceListener.getClass());
                } catch (RRepositoryException e) {
                    throw new DataSourceException(e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeAllListeners() {
        synchronized (listeners) {
            RProductionRepository rProductionRepository = getRProductionRepository();
            for (DataSourceListener dataSourceListener : listeners.keySet()) {
                RDeploymentListener listener = listeners.get(dataSourceListener);
                if (listener != null) {
                    try {
                        rProductionRepository.removeListener(listener);
                        listeners.remove(dataSourceListener);
                        log.info("{} class listener is removed from jcr data source", dataSourceListener.getClass());
                    } catch (RRepositoryException e) {
                        throw new DataSourceException(e);
                    }
                }
            }
        }
    }

    public void destroy() throws Exception {
        log.debug("JCR data source releasing");
        productionRepositoryFactoryProxy.releaseRepository(repositoryPropertiesFile);

        if (shouldDestroyProxy) {
            productionRepositoryFactoryProxy.destroy();
        }
    }

    public void setProductionRepositoryFactoryProxy(ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy) {
        this.productionRepositoryFactoryProxy = productionRepositoryFactoryProxy;
    }

    public ProductionRepositoryFactoryProxy getProductionRepositoryFactoryProxy() {
        if (productionRepositoryFactoryProxy == null) {
            // Lazy initialization for backward compatibility
            productionRepositoryFactoryProxy = new ProductionRepositoryFactoryProxy();
            // We create a proxy (not spring container) - that's why we should
            // destroy it
            shouldDestroyProxy = true;
        }
        return productionRepositoryFactoryProxy;
    }

    public void setRepositoryPropertiesFile(String repositoryPropertiesFile) {
        this.repositoryPropertiesFile = repositoryPropertiesFile;
    }

    public String getRepositoryPropertiesFile() {
        return repositoryPropertiesFile;
    }

    private RDeploymentListener buildRDeploymentListener(DataSourceListener dataSourceListener) {
        return new DataSourceListenerWrapper(dataSourceListener);
    }

    private static class DataSourceListenerWrapper implements RDeploymentListener {
        private DataSourceListener dataSourceListener;

        public DataSourceListenerWrapper(DataSourceListener dataSourceListener) {
            if (dataSourceListener == null) {
                throw new IllegalArgumentException();
            }
            this.dataSourceListener = dataSourceListener;
        }

        public void onEvent() {
            dataSourceListener.onDeploymentAdded();
        }

        @Override
        public int hashCode() {
            return dataSourceListener.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DataSourceListenerWrapper) {
                DataSourceListenerWrapper dataSourceListenerWrapper = (DataSourceListenerWrapper) obj;
                return this.dataSourceListener.equals(dataSourceListenerWrapper.dataSourceListener);
            }
            return false;
        }
    }
}
