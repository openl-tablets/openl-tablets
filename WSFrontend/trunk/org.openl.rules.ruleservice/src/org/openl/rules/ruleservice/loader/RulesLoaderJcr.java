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
import org.openl.SmartProps;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.production.client.JcrRulesClient;

/**
 * Loads deployments from JCR runtime repository.
 *
 * @author Sergey Zyrianov
 *
 */
public class RulesLoaderJcr implements RulesLoader, RDeploymentListener {
    private static final Log log = LogFactory.getLog(RulesLoaderJcr.class);

    private JcrRulesClient rulesClient;
    private File tempFolder;

    private ArrayList<LoadingListener> loadingListeners = new ArrayList<LoadingListener>();

    private Map<String, String> deployment2Version = new HashMap<String, String>();
    
    public RulesLoaderJcr(JcrRulesClient jcrRulesClient) throws RRepositoryException {
        setRulesClient(jcrRulesClient);
        setTempFolder(new File(getTempDirectory()));
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

    public void addLoadingListener(LoadingListener loadingListener) {
        if (loadingListener != null) {
            loadingListeners.add(loadingListener);
        }
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
                // check whether deployment with latest version has not been
                // loaded yet
                if (!version.equals(deployment2Version.get(deployment.getName()))) {
                    deploymentsToLoad.add(deployment);
                }
            }
        }

        return deploymentsToLoad;
    }

    private File downloadDeployment(DeploymentInfo di) throws Exception {
        File deploymentFolder = new File(tempFolder, di.getName());

        rulesClient.fetchDeployment(di.getDeployID(), deploymentFolder);
        return deploymentFolder;
    }

    public synchronized void load(DeploymentInfo di) throws Exception {
        log.debug(String.format("Start loading deployment \"{%s}\"", di.getName()));
        onBeforeLoading(di);

        try {
            File deploymentLocalFolder = downloadDeployment(di);

            deployment2Version.put(di.getName(), di.getVersion().getVersionName());

            log.info(String.format("Loaded deployment \"{%s}\" at {%s}", di.getName(), deploymentLocalFolder
                    .getAbsolutePath()));

            onAfterLoading(di, deploymentLocalFolder);
        } catch (Exception e) {
            log.error("failed to load deployment project " + di.getDeployID(), e);
            throw e;
        }
    }

    public synchronized void loadRules() {
        try {
            Collection<String> deploymentNames;

            deploymentNames = rulesClient.getDeploymentNames();

            Collection<DeploymentInfo> deployments = parseDeloyments(deploymentNames);

            Map<String, CommonVersion> latestVersionMap = computeLatestVersions(deployments);

            deployments = computeDeploymentsToLoad(deployments, latestVersionMap);

            for (DeploymentInfo deployment : deployments) {
                load(deployment);
            }
        } catch (Exception e) {
            log.error("Exception loading new items", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void onAfterLoading(DeploymentInfo di, File deploymentLocalFolder) {
        Collection<LoadingListener> localCopyOfListeners = (Collection<LoadingListener>) loadingListeners.clone();

        for (LoadingListener loadingListener : localCopyOfListeners) {
            loadingListener.onAfterLoading(new LoadingEventObject(this, di, deploymentLocalFolder));
        }
    }

    @SuppressWarnings("unchecked")
    private void onBeforeLoading(DeploymentInfo di) {
        Collection<LoadingListener> localCopyOfListeners = (Collection<LoadingListener>) loadingListeners.clone();

        for (LoadingListener loadingListener : localCopyOfListeners) {
            loadingListener.onBeforeLoading(new LoadingEventObject(this, di));
        }
    }

    public void removeLoadingListener(LoadingListener loadingListener) {
        loadingListeners.remove(loadingListener);
    }

    public void resetLoadedDeploymentsCash() {
        deployment2Version = new HashMap<String, String>();
    }

    private void setRulesClient(JcrRulesClient rulesJcrClient) throws RRepositoryException {
        this.rulesClient = rulesJcrClient;
        final RulesLoaderJcr loader = this;
        rulesClient.addListener(loader);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                try {
                    rulesClient.removeListener(loader);
                } catch (RRepositoryException e) {
                }

                try {
                    rulesClient.release();
                } catch (RRepositoryException e) {
                }
            }
        }));
    }

    /**
     * Gets path to temporary directory. Extract the value with key
     * <i>ruleservice.tmp.dir</i> from configuration file. If such a key is
     * missing returns default value <tt>/tmp/rules-deploy</tt>.
     *
     * @return path to temporary directory
     */
    protected  String getTempDirectory() {
        SmartProps props = new SmartProps("rules-production.properties");
        String value = props.getStr("ruleservice.tmp.dir");
        if (value == null || value.trim().length() == 0) {
            return "/tmp/rules-deploy";
        }

        return value;
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

    public void projectsAdded() {
        loadRules();
    }
}
