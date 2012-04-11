/**
 *
 */
package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.SmartProps;
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
public class JcrRulesLoader implements RulesLoader, RDeploymentListener {
    private static final Log LOG = LogFactory.getLog(JcrRulesLoader.class);

    private JcrRulesClient rulesClient;
    private String folderToLoadIn;
    private DeploymentsToLoadManager deploymentsToLoadManager;
    
    private File folderToLoadDeploymentsIn;

    private ArrayList<LoadingListener> loadingListeners = new ArrayList<LoadingListener>();

    public JcrRulesLoader(JcrRulesClient jcrRulesClient) throws RRepositoryException {
        setRulesClient(jcrRulesClient);
        getFolderToLoadDeploymentsIn();
    }
    
    public JcrRulesLoader(JcrRulesClient jcrRulesClient, String theFolderToLoadIn) throws RRepositoryException {
        setRulesClient(jcrRulesClient);
        folderToLoadIn = theFolderToLoadIn;
    }
    
    public DeploymentsToLoadManager getDeploymentsToLoadManager() {
        return deploymentsToLoadManager;
    }

    public void setDeploymentsToLoadManager(DeploymentsToLoadManager deploymentsToLoadManager) {
        this.deploymentsToLoadManager = deploymentsToLoadManager;
    }

    public void projectsAdded() {
        loadRules();
    }
    
    public synchronized void loadRules() {
        try {
            Collection<String> deploymentNames = rulesClient.getDeploymentNames();

            Collection<DeploymentInfo> deployments = parseDeloyments(deploymentNames);

            Collection<DeploymentInfo> deploymentsToLoad = deploymentsToLoadManager.getDeploymentsToLoad(deployments);

            for (DeploymentInfo deployment : deploymentsToLoad) {
                load(deployment);
            }
        } catch (Exception e) {
            LOG.error("Exception loading new items", e);
        }
    }
    
    public void resetLoadedDeploymentsCache() {
        if (deploymentsToLoadManager != null) {
            deploymentsToLoadManager.resetLoadedDeploymentsCache();
        }
    }
    
    public synchronized void load(DeploymentInfo di) throws Exception {
        LOG.debug(String.format("Start loading deployment \"%s\"", getDeploymentFolderName(di)));
        onBeforeLoading(di);

        try {
            File deploymentLocalFolder = downloadDeployment(di);

            LOG.info(String.format("Loaded deployment \"%s\" at %s", getDeploymentFolderName(di), deploymentLocalFolder
                    .getAbsolutePath()));

            onAfterLoading(di, deploymentLocalFolder);
        } catch (Exception e) {
            LOG.error("Failed to load deployment project " + di.getDeployID(), e);
            throw e;
        }
    }
    
    public void resetFolderToLoadDeploymentsIn() {
        folderToLoadDeploymentsIn = null;
    }
    
    /*internal for test*/ static Collection<DeploymentInfo> parseDeloyments(Collection<String> deploymentNames) {
        Collection<DeploymentInfo> deployments = new ArrayList<DeploymentInfo>();

        for (String deploymentName : deploymentNames) {
            DeploymentInfo di = DeploymentInfo.valueOf(deploymentName);
            if (di != null) {
                deployments.add(di);
            }
        }
        return deployments;
    }

    private File downloadDeployment(DeploymentInfo di) throws Exception {
        File deploymentFolder = new File(getFolderToLoadDeploymentsIn(), getDeploymentFolderName(di));

        rulesClient.fetchDeployment(di.getDeployID(), deploymentFolder);
        return deploymentFolder;
    }

    private String getDeploymentFolderName(DeploymentInfo di) {
        return String.format("%s_v%s", di.getName(), di.getVersion().getVersionName());
    }

    private void setRulesClient(JcrRulesClient rulesJcrClient) throws RRepositoryException {
        rulesClient = rulesJcrClient;
        
        final JcrRulesLoader loader = this;
        
        // Subscribe to deployment update notifications from JCR and properly disconnect on shutdown
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

    private File getFolderToLoadDeploymentsIn() {
        if (folderToLoadDeploymentsIn == null) {
            if (folderToLoadIn == null) {
                folderToLoadIn = getTempDirectory();
            }
            
            folderToLoadDeploymentsIn = new File(folderToLoadIn);
    
            folderToLoadDeploymentsIn.mkdirs();
            FolderHelper.clearFolder(folderToLoadDeploymentsIn);
    
            resetLoadedDeploymentsCache();
        }
        
        return folderToLoadDeploymentsIn;
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

    public void addLoadingListener(LoadingListener loadingListener) {
        if (loadingListener != null) {
            loadingListeners.add(loadingListener);
        }
    }


    public void removeLoadingListener(LoadingListener loadingListener) {
        loadingListeners.remove(loadingListener);
    }
}
