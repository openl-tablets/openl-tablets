package org.openl.rules.ruleservice.publish;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.main.OpenLWrapper;
import org.openl.rules.ruleservice.instantiation.CglibInstantiationStrategy;
import org.openl.rules.ruleservice.instantiation.EngineFactoryInstantiationStrategy;
import org.openl.rules.ruleservice.instantiation.InstantiationStrategy;
import org.openl.rules.ruleservice.resolver.RuleServiceInfo;

public class JavaClassDeploymentAdmin implements DeploymentAdmin {
    private static final Log log = LogFactory.getLog(WebServicesDeployAdmin.class);

    private Map<String, Map<String, OpenLWrapper>> runningServices = new HashMap<String, Map<String, OpenLWrapper>>();

    public synchronized void deploy(String deploymentName, ClassLoader loader, List<RuleServiceInfo> infoList) {
        onBeforeDeployment(deploymentName);
        
        undeploy(deploymentName);

        Map<String, OpenLWrapper> projectWrappers = new HashMap<String, OpenLWrapper>();
        for (RuleServiceInfo wsInfo : infoList) {
            try {
                OpenLWrapper wrapper = deploy(deploymentName, loader, wsInfo);
                projectWrappers.put(wsInfo.getName(), wrapper);
            } catch (Exception e) {
                log.error("failed to create service", e);
            }
        }

        runningServices.put(deploymentName, projectWrappers);
        log.info(String.format("Deployed \"{1}\" ", deploymentName));
        
        onAfterDeployment(deploymentName, projectWrappers);
    }

    public synchronized void undeploy(String deploymentName) {
        onBeforeUndeployment(deploymentName);
        
        runningServices.remove(deploymentName);
        
        onAfterUndeployment(deploymentName);
    }

    private OpenLWrapper deploy(String serviceName, ClassLoader loader, RuleServiceInfo wsInfo)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<?> aClass = loader.loadClass(wsInfo.getClassName());

        return (OpenLWrapper) getStrategy(wsInfo).instantiate(aClass);

    }
    
    private InstantiationStrategy getStrategy(RuleServiceInfo wsInfo) {
        if (wsInfo.isUsingEngineFactory()) {
            return new EngineFactoryInstantiationStrategy(wsInfo.getXlsFile());
        } else {
            String path = ".";
            try {
                path = wsInfo.getProject().getCanonicalPath();
            } catch (IOException e) {
                log.error("failed to get canonical path", e);
            }
            return new CglibInstantiationStrategy(path);
        }
    }
    
    private Collection<DeploymentListener> deploymentListeners = new ArrayList<DeploymentListener>();

    public void addDeploymentListener(DeploymentListener deploymentListener) {
        if (deploymentListener != null){
            deploymentListeners.add(deploymentListener);
        }
        
    }
    
    public void removeDeploymentListener(DeploymentListener deploymentListener) {
        deploymentListeners.remove(deploymentListener);        
    }
    
    private void onBeforeDeployment(String deploymentName){
        for (DeploymentListener deploymentListener : deploymentListeners){
            deploymentListener.beforeDeployment(deploymentName);
        }
    }
    
    private void onAfterDeployment(String deploymentName, Map<String, OpenLWrapper> projectWrappers){
        for (DeploymentListener deploymentListener : deploymentListeners){
            deploymentListener.afterDeployment(deploymentName, projectWrappers);
        }
    }
    
    private void onBeforeUndeployment(String deploymentName){
        for (DeploymentListener deploymentListener : deploymentListeners){
            deploymentListener.beforeUndeployment(deploymentName);
        }
    }
    
    private void onAfterUndeployment(String deploymentName){
        for (DeploymentListener deploymentListener : deploymentListeners){
            deploymentListener.afterUndeployment(deploymentName);
        }
    }

}