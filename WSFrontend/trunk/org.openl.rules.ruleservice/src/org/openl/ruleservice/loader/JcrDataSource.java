package org.openl.ruleservice.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrDataSource implements IDataSource {
	private static final String SEPARATOR = "#";

	private Map<DataSourceListener, RDeploymentListener> listeners = new HashMap<DataSourceListener, RDeploymentListener>();

	public List<Deployment> getDeployments() throws DataSourceException {
		try {
			List<FolderAPI> deploymentProjects = ProductionRepositoryFactoryProxy
					.getRepositoryInstance().getDeploymentProjects();
			List<Deployment> ret = new ArrayList<Deployment>();
			for (FolderAPI deploymentProject : deploymentProjects) {
				ret.add(new Deployment(deploymentProject));
			}
			return ret;
		} catch (RRepositoryException e) {
			throw new DataSourceException(e);
		}
	}

	public Deployment getDeployment(String deploymentName,
			CommonVersion deploymentVersion) throws DataSourceException {
		if (deploymentName == null)
			throw new IllegalArgumentException();
		try {
			StringBuilder sb = new StringBuilder(deploymentName);
			sb.append(SEPARATOR);
			sb.append(deploymentVersion.getVersionName());
			FolderAPI deploymentProject = ProductionRepositoryFactoryProxy
					.getRepositoryInstance()
					.getDeploymentProject(sb.toString());
			return new Deployment(deploymentProject, deploymentName,
					deploymentVersion);
		} catch (RRepositoryException e) {
			throw new DataSourceException(e);
		}
	}

	public List<DataSourceListener> getListeners() throws DataSourceException {
		List<DataSourceListener> tmp = null;
		synchronized (listeners) {
			Set<DataSourceListener> _listeners = listeners.keySet();
			tmp = new ArrayList<DataSourceListener>(_listeners);
		}
		return Collections.unmodifiableList(tmp);
	}

	public void addListener(DataSourceListener dataSourceListener)
			throws DataSourceException {
		if (dataSourceListener == null)
			throw new IllegalArgumentException();
		synchronized (listeners) {
			RDeploymentListener rDeploymentListener = listeners
					.get(dataSourceListener);
			if (rDeploymentListener == null) {
				rDeploymentListener = buildRDeploymentListener(dataSourceListener);
				try {
					ProductionRepositoryFactoryProxy.getRepositoryInstance()
							.addListener(rDeploymentListener);
					listeners.put(dataSourceListener, rDeploymentListener);
				} catch (RRepositoryException e) {
					throw new DataSourceException(e);
				}
			}
		}
	}

	public void removeListener(DataSourceListener dataSourceListener)
			throws DataSourceException {
		if (dataSourceListener == null)
			throw new IllegalArgumentException();
		synchronized (listeners) {
			RDeploymentListener listener = listeners.get(dataSourceListener);
			if (listener != null) {
				try {
					ProductionRepositoryFactoryProxy.getRepositoryInstance()
							.removeListener(listener);
					listeners.remove(dataSourceListener);
				} catch (RRepositoryException e) {
					throw new DataSourceException(e);
				}
			}
		}
	}

	private RDeploymentListener buildRDeploymentListener(
			DataSourceListener dataSourceListener) {
		return new DataSourceListenerWrapper(dataSourceListener);
	}

	private static class DataSourceListenerWrapper implements
			RDeploymentListener {
		private DataSourceListener dataSourceListener;

		public DataSourceListenerWrapper(DataSourceListener dataSourceListener) {
			if (dataSourceListener == null)
				throw new IllegalArgumentException();
			this.dataSourceListener = dataSourceListener;
		}

		public void projectsAdded() {
			dataSourceListener.onDeploymentAdded();
		}
	}
}
