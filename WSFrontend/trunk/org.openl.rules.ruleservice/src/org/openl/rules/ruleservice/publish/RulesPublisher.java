package org.openl.rules.ruleservice.publish;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.ruleservice.loader.DeploymentInfo;
import org.openl.rules.ruleservice.resolver.RulesProjectResolver;
import org.openl.rules.ruleservice.resolver.RulesProjectInfo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RulesPublisher {
    private final Log log = LogFactory.getLog(getClass());

    private DeploymentAdmin deployAdmin;
    private RulesProjectResolver rulesProjectResolver;
    private ServiceNameBuilder serviceNameBuilder;
    
    private Map<String, ClassLoader> deployment2ClassLoader = new HashMap<String, ClassLoader>();

    public synchronized void deploy(DeploymentInfo di, File deploymentLocalFolder) {
        try {
            List<RulesProjectInfo> serviceClasses = rulesProjectResolver.resolve(deploymentLocalFolder);
            
            URLClassLoader urlClassLoader = createDeploymentClassLoader(serviceClasses);

            String serviceName = serviceNameBuilder.getServiceName(di);
            
            deployment2ClassLoader.put(serviceName, urlClassLoader);
            deployAdmin.deploy(serviceName, urlClassLoader, serviceClasses);
        } catch (Exception e) {
            log.error(String.format("Failed to deploy project \"%s\"", di.getDeployID()), e);
        }
    }

    public synchronized void undeploy(DeploymentInfo di) {
        if (deployment2ClassLoader.remove(serviceNameBuilder.getServiceName(di)) != null) {
            deployAdmin.undeploy(di.getName());
        }
    }
    
    private URLClassLoader createDeploymentClassLoader(List<RulesProjectInfo> serviceClasses) {
        List<URL> classPathURLs = new ArrayList<URL>();

        for (RulesProjectInfo serviceInfo : serviceClasses) {
            addClasspathURL(classPathURLs, serviceInfo.getProjectBin());
        }

        URLClassLoader urlClassLoader = new URLClassLoader(classPathURLs.toArray(new URL[classPathURLs.size()]),
                Thread.currentThread().getContextClassLoader());
        return urlClassLoader;
    }
    
    private void addClasspathURL(List<URL> classPathURLs, File folder) {
        try {
            classPathURLs.add(new URL("file:" + folder.getCanonicalPath() + "/"));
        } catch (IOException e) {
            log.error("Failed to get classpath URL while publishing deployment", e);
        }
    }
    
    public void setDeployAdmin(DeploymentAdmin deployAdmin) {
        this.deployAdmin = deployAdmin;
    }

    public DeploymentAdmin getDeployAdmin() {
        return deployAdmin;
    }

    public synchronized void setRulesProjectResolver(RulesProjectResolver rulesProjectResolver) {
        this.rulesProjectResolver = rulesProjectResolver;
    }

    public ServiceNameBuilder getServiceNameBuilder() {
        return serviceNameBuilder;
    }

    public void setServiceNameBuilder(ServiceNameBuilder serviceNameBuilder) {
        this.serviceNameBuilder = serviceNameBuilder;
    }
}
