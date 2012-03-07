package org.openl.rules.ruleservice.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.beans.factory.DisposableBean;

/**
 * JCR repository data source. Uses
 * ProductionRepositoryFactoryProxy.getRepositoryInstance() repository. Thread
 * safe implementation.
 * 
 * @author Marat Kamalov
 * 
 */
public class JcrDataSource implements DataSource, DisposableBean {
    private Log log = LogFactory.getLog(JcrDataSource.class);

    private static final String SEPARATOR = "#";

    private Map<DataSourceListener, RDeploymentListener> listeners = new HashMap<DataSourceListener, RDeploymentListener>();

    /** {@inheritDoc} */
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
            if (log.isWarnEnabled()) {
                log.warn("Exception has been occured on deployments retriving from repository", e);
            }
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
            log.debug(String.format("Getting deployement with name=\"%s\" and version=\"%s\"", deploymentName,
                    deploymentVersion.getVersionName()));
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
                log.warn(
                        String.format("Exception has been occured on deployment retriving from repository. "
                                + "Deployment name is \"%s\". Deployment version is \"%s\". "
                                + "Deployment with this name may be doesn't exists.", deploymentName,
                                deploymentVersion.getVersionName()), e);
            }
            throw new DataSourceException(e);
        }
    }

    /** {@inheritDoc} */
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
            rProductionRepository = ProductionRepositoryFactoryProxy.getRepositoryInstance();
            return rProductionRepository;
        } catch (RRepositoryException e) {
            if (log.isErrorEnabled()) {
                log.error("Exception has been occured on getting instance of RProductionRepository.", e);
            }
            throw new DataSourceException(e);
        }
    }

    /** {@inheritDoc} */
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
                    if (log.isInfoEnabled()) {
                        log.info(dataSourceListener.getClass().toString()
                                + " class listener is registered in jcr data source");
                    }
                } catch (RRepositoryException e) {
                    if (log.isWarnEnabled()) {
                        log.warn("Exception has been occured on adding listener to jcr data source.", e);
                    }
                    throw new DataSourceException(e);
                }
            }
        }
    }

    /** {@inheritDoc} */
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
                    if (log.isInfoEnabled()) {
                        log.info(dataSourceListener.getClass().toString()
                                + " class listener is unregistered from jcr data source");
                    }
                } catch (RRepositoryException e) {
                    if (log.isWarnEnabled()) {
                        log.warn("Exception has been occured on removing listener from jcr data source.", e);
                    }
                    throw new DataSourceException(e);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public void removeAllListeners() {
        synchronized (listeners) {
            RProductionRepository rProductionRepository = getRProductionRepository();
            for (DataSourceListener dataSourceListener : listeners.keySet()) {
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
                        if (log.isWarnEnabled()) {
                            log.warn("Exception has been occured on removing listener from jcr data source.", e);
                        }
                        throw new DataSourceException(e);
                    }
                }
            }
        }
    }

    public void destroy() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("JCR data source releasing");
        }
        getRProductionRepository().release();
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

        public void projectsAdded() {
            dataSourceListener.onDeploymentAdded();
        }
    }
}
