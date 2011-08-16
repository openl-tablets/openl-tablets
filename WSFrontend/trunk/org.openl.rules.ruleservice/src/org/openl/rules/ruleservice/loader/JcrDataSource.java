package org.openl.rules.ruleservice.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * JCR repository data source. Uses
 * ProductionRepositoryFactoryProxy.getRepositoryInstance() repository.
 * Thread safe implementation.
 * 
 * @author MKamalov
 * 
 */
public class JcrDataSource implements IDataSource {
    private Log log = LogFactory.getLog(JcrDataSource.class);

    private static final String SEPARATOR = "#";

    private Map<IDataSourceListener, RDeploymentListener> listeners = new HashMap<IDataSourceListener, RDeploymentListener>();

    /** {@inheritDoc} */
    public List<Deployment> getDeployments() {
        try {
            List<FolderAPI> deploymentProjects = getRProductionRepository().getDeploymentProjects();
            List<Deployment> ret = new ArrayList<Deployment>();
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
            log.warn("Exception has been occured on deployments retriving from repository", e);
            throw new DataSourceException(e);
        }
    }

    /** {@inheritDoc} */
    public Deployment getDeployment(String deploymentName, CommonVersion deploymentVersion) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName argument can't be null");
        }
        if (deploymentVersion == null) {
            throw new IllegalArgumentException("deploymentVersion argument can't be null");
        }

        if (log.isDebugEnabled()) {
            log.debug("Getting deployement with name=" + deploymentName + " and version="
                    + deploymentVersion.getVersionName());
        }

        try {
            StringBuilder sb = new StringBuilder(deploymentName);
            sb.append(SEPARATOR);
            sb.append(deploymentVersion.getVersionName());
            // FIXME
            // Should be deploymentNotFoundException or null return
            FolderAPI deploymentProject = getRProductionRepository().getDeploymentProject(sb.toString());
            return new Deployment(deploymentProject, deploymentName, deploymentVersion);
        } catch (RRepositoryException e) {
            if (log.isWarnEnabled()) {
                log.warn("Exception has been occured on deployment retriving from repository. Deployment name is "
                        + deploymentName + ". Deployment version is " + deploymentVersion.getVersionName()
                        + ". Deployment with this name may be doesn't exists.", e);
            }
            throw new DataSourceException(e);
        }
    }

    /** {@inheritDoc} */
    public List<IDataSourceListener> getListeners() {
        List<IDataSourceListener> tmp = null;
        synchronized (listeners) {
            Set<IDataSourceListener> dataSourceListeners = listeners.keySet();
            tmp = new ArrayList<IDataSourceListener>(dataSourceListeners);
        }
        return Collections.unmodifiableList(tmp);
    }

    private RProductionRepository getRProductionRepository() {
        RProductionRepository rProductionRepository = null;
        try {
            rProductionRepository = ProductionRepositoryFactoryProxy.getRepositoryInstance();
            return rProductionRepository;
        } catch (RRepositoryException e) {
            log.error("Exception has been occured on getting instance of RProductionRepository.", e);
            throw new DataSourceException(e);
        }
    }

    /** {@inheritDoc} */
    public void addListener(IDataSourceListener dataSourceListener) {
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
                    if (log.isInfoEnabled()) {
                        log.info(dataSourceListener.getClass().toString()
                                + " class listener is registered in jcr data source");
                    }
                } catch (RRepositoryException e) {
                    log.warn("Exception has been occured on adding listener to jcr data source.", e);
                    throw new DataSourceException(e);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public void removeListener(IDataSourceListener dataSourceListener) {
        if (dataSourceListener == null) {
            throw new IllegalArgumentException("dataSourceListener argument can't be null");
        }
        synchronized (listeners) {
            RDeploymentListener listener = listeners.get(dataSourceListener);
            if (listener != null) {
                try {
                    getRProductionRepository().removeListener(listener);
                    listeners.remove(dataSourceListener);
                    if (log.isInfoEnabled()) {
                        log.info(dataSourceListener.getClass().toString()
                                + " class listener is unregistered from jcr data source");
                    }
                } catch (RRepositoryException e) {
                    log.warn("Exception has been occured on removing listener from jcr data source.", e);
                    throw new DataSourceException(e);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public void removeAllListeners() {
        synchronized (listeners) {
            RProductionRepository rProductionRepository = getRProductionRepository();
            for (IDataSourceListener dataSourceListener : listeners.keySet()) {
                RDeploymentListener listener = listeners.get(dataSourceListener);
                if (listener != null) {
                    try {
                        rProductionRepository.removeListener(listener);
                        listeners.remove(dataSourceListener);
                        if (log.isInfoEnabled()) {
                            log.info(dataSourceListener.getClass().toString()
                                    + " class listener is removed from jcr data source");
                        }
                    } catch (RRepositoryException e) {
                        log.warn("Exception has been occured on removing listener from jcr data source.", e);
                        throw new DataSourceException(e);
                    }
                }
            }
        }
    }

    private RDeploymentListener buildRDeploymentListener(IDataSourceListener dataSourceListener) {
        return new DataSourceListenerWrapper(dataSourceListener);
    }

    private static class DataSourceListenerWrapper implements RDeploymentListener {
        private IDataSourceListener dataSourceListener;

        public DataSourceListenerWrapper(IDataSourceListener dataSourceListener) {
            if (dataSourceListener == null) {
                throw new IllegalArgumentException();
            }
            this.dataSourceListener = dataSourceListener;
        }

        public void projectsAdded() {
            dataSourceListener.onDeploymentAdded();
        }
    }
}
