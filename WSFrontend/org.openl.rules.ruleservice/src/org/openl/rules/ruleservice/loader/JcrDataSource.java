package org.openl.rules.ruleservice.loader;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PreDestroy;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.deploy.DeployUtils;
import org.openl.util.RuntimeExceptionWrapper;
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
    private boolean includeVersionInDeploymentName = false;

    /**
     * {@inheritDoc}
     */
    public Collection<Deployment> getDeployments() {
        Collection<FileData> fileDatas;
        try {
            fileDatas = repository.list(DeployUtils.DEPLOY_PATH);
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        ConcurrentMap<String, Deployment> deployments = new ConcurrentHashMap<String, Deployment>();
        for (FileData fileData : fileDatas) {
            String deploymentFolderName = fileData.getName().substring(DeployUtils.DEPLOY_PATH.length()).split("/")[0];
            String deploymentName = deploymentFolderName;
            CommonVersionImpl commonVersion = null;

            if (includeVersionInDeploymentName) {
                int separatorPosition = deploymentFolderName.lastIndexOf(DeployUtils.SEPARATOR);

                if (separatorPosition >= 0) {
                    deploymentName = deploymentFolderName.substring(0, separatorPosition);
                    int version = Integer.valueOf(deploymentFolderName.substring(separatorPosition + 1));
                    commonVersion = new CommonVersionImpl(version);
                } else {
                    commonVersion = null;
                }
            } else {
                String version = fileData.getVersion();
                if (version != null) {
                    commonVersion = new CommonVersionImpl(version);
                }
            }
            Deployment deployment = new Deployment(repository,
                DeployUtils.DEPLOY_PATH + deploymentFolderName,
                deploymentName,
                commonVersion, false);
            deployments.putIfAbsent(deploymentFolderName, deployment);
        }

        return deployments.values();
    }

    /**
     * {@inheritDoc}
     */
    public Deployment getDeployment(String deploymentName, CommonVersion deploymentVersion) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName argument must not be null.");
        }
        if (deploymentVersion == null) {
            throw new IllegalArgumentException("deploymentVersion argument must not be null.");
        }

        log.debug("Getting deployement with name='{}' and version='{}'",
            deploymentName,
            deploymentVersion.getVersionName());

        String name;
        if (includeVersionInDeploymentName) {
            name = deploymentName + DeployUtils.SEPARATOR + deploymentVersion.getVersionName();
        } else {
            name = deploymentName;
        }
        return new Deployment(repository, DeployUtils.DEPLOY_PATH + name, deploymentName, deploymentVersion, false);
    }

    /**
     * {@inheritDoc}
     */
    public void setListener(final DataSourceListener dataSourceListener) {
        if (dataSourceListener == null) {
            repository.setListener(null);
        } else {
            repository.setListener(new Listener() {
                @Override
                public void onChange() {
                    dataSourceListener.onDeploymentAdded();
                }
            });
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

    public void setIncludeVersionInDeploymentName(boolean includeVersionInDeploymentName) {
        this.includeVersionInDeploymentName = includeVersionInDeploymentName;
    }
}
