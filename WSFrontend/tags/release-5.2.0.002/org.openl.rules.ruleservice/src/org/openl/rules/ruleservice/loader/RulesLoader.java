/**
 * 
 */
package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.production.client.JcrRulesClient;

/**
 * Loads deployments from runtime repository.
 * 
 * @author Sergey Zyrianov
 * 
 */
public class RulesLoader implements Runnable {
    private static final Log log = LogFactory.getLog(RulesLoader.class);

    private JcrRulesClient rulesClient;
    private File tempFolder;

    private ArrayList<LoadingListener> loadingListeners = new ArrayList<LoadingListener>();

    private Map<String, String> deployment2Version = new HashMap<String, String>();

    public void setRulesClient(JcrRulesClient rulesClient) {
        this.rulesClient = rulesClient;
    }

    public void setTempFolder(File tempFolder) {
        if (tempFolder == null) {
            throw new IllegalArgumentException("Argument tempFolder can't be null");
        }
        
        this.tempFolder = tempFolder;

        tempFolder.mkdirs();
        FolderHelper.clearFolder(tempFolder);

        resetLoadedDeploymentsCash();
    }

    public void resetLoadedDeploymentsCash() {
        deployment2Version = new HashMap<String, String>();
    }

    public void addLoadingListener(LoadingListener loadingListener) {
        if (loadingListener != null) {
            loadingListeners.add(loadingListener);
        }
    }

    public void removeLoadingListener(LoadingListener loadingListener) {
        loadingListeners.remove(loadingListener);
    }

    /**
     * 
     */
    public void run() {
        try {
            loadRules();
        } catch (Exception e) {
            log.error("Exception loading new items", e);
        }
    }

    public synchronized void loadRules() throws RRepositoryException, Exception {
        Collection<String> deploymentNames;

        try {
            deploymentNames = rulesClient.getDeploymentNames();
        } catch (RRepositoryException e) {
            log.error("failed to get deployment names ", e);
            throw e;
        }

        Collection<DeploymentInfo> deployments = parseDeloyments(deploymentNames);

        Map<String, CommonVersion> latestVersionMap = computeLatestVersions(deployments);

        deployments = computeDeploymentsToLoad(deployments, latestVersionMap);

        for (DeploymentInfo deployment : deployments) {
            load(deployment);
        }
    }

    public synchronized void load(DeploymentInfo di) throws Exception {
        log.debug(String.format("Start loading deployment \"{1}\"", di.getName()));
        onBeforeLoading(di);

        try {
            File deploymentLocalFolder = downloadDeployment(di);

            deployment2Version.put(di.getName(), di.getVersion().getVersionName());

            log.info(String.format("Loaded deployment \"{1}\" at {2}", di.getName(), deploymentLocalFolder.getAbsolutePath()));
            
            onAfterLoading(di, deploymentLocalFolder);
        } catch (Exception e) {
            log.error("failed to load deployment project " + di.getDeployID(), e);
            throw e;
        }
    }

    private File downloadDeployment(DeploymentInfo di) throws Exception {
        File deploymentFolder = new File(tempFolder, di.getName());

        rulesClient.fetchDeployment(di.getDeployID(), deploymentFolder);
        return deploymentFolder;
    }

    public static Collection<DeploymentInfo> parseDeloyments(Collection<String> deploymentNames) {
        Collection<DeploymentInfo> deployments = new ArrayList<DeploymentInfo>();

        for (String deploymentName : deploymentNames) {
            DeploymentInfo di = DeploymentInfo.valueOf(deploymentName);
            if (di != null) {
                deployments.add(di);
            }
        }
        return deployments;
    }

    /**
     * @param deployments
     * @return
     */
    public static Map<String, CommonVersion> computeLatestVersions(Collection<DeploymentInfo> deployments) {
        Map<String, CommonVersion> versionMap = new HashMap<String, CommonVersion>();

        for (DeploymentInfo deployment : deployments) {
            CommonVersion version = versionMap.get(deployment.getName());

            if (version == null || deployment.getVersion().compareTo(version) > 0) {
                version = deployment.getVersion();
            }

            versionMap.put(deployment.getName(), version);
        }

        return versionMap;
    }

    /**
     * @param deployments
     * @param latestVersionMap
     */
    private Collection<DeploymentInfo> computeDeploymentsToLoad(Collection<DeploymentInfo> deployments,
            Map<String, CommonVersion> latestVersionMap) {
        Collection<DeploymentInfo> deploymentsToLoad = new ArrayList<DeploymentInfo>();

        for (DeploymentInfo deployment : deployments) {
            // check whether we load only deployment with latest version
            if (latestVersionMap.get(deployment.getName()).equals(deployment.getVersion())) {
                final String version = deployment.getVersion().getVersionName();
                // check whether deployment with latest version has not been loaded yet
                if (!version.equals(deployment2Version.get(deployment.getName()))) {
                    deploymentsToLoad.add(deployment);
                }
            }
        }

        return deploymentsToLoad;
    }

    @SuppressWarnings("unchecked")
    private void onBeforeLoading(DeploymentInfo di) {
        Collection<LoadingListener> localCopyOfListeners = (Collection<LoadingListener>) loadingListeners.clone();
        
        for (LoadingListener loadingListener : localCopyOfListeners) {
            loadingListener.onBeforeLoading(new LoadingEventObject(this, di));
        }
    }

    @SuppressWarnings("unchecked")
    private void onAfterLoading(DeploymentInfo di, File deploymentLocalFolder) {
        Collection<LoadingListener> localCopyOfListeners = (Collection<LoadingListener>) loadingListeners.clone();
        
        for (LoadingListener loadingListener : localCopyOfListeners) {
            loadingListener.onAfterLoading(new LoadingEventObject(this, di, deploymentLocalFolder));
        }
    }

}
