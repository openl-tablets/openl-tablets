package org.openl.rules.ruleservice.publish;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.ruleservice.loader.DeploymentInfo;
import org.openl.rules.ruleservice.resolver.RulesProjectResolver;
import org.openl.rules.ruleservice.resolver.RuleServiceInfo;

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

    private Map<String, ClassLoader> deployment2ClassLoader = new HashMap<String, ClassLoader>();

    private DeploymentAdmin deployAdmin;
    private RulesProjectResolver rulesProjectResolver;

    private void addClasspathURL(List<URL> classPathURLs, File folder) {
        try {
            classPathURLs.add(new URL("file:" + folder.getCanonicalPath() + "/"));
        } catch (IOException e) {
            log.error("could not create classpath URL", e);
        }
    }

    public synchronized void deploy(DeploymentInfo di, File deploymentLocalFolder) {

        try {
            List<RuleServiceInfo> serviceClasses = rulesProjectResolver.resolve(di, deploymentLocalFolder);
            List<URL> classPathURLs = new ArrayList<URL>();

            for (RuleServiceInfo wsInfo : serviceClasses) {

                addClasspathURL(classPathURLs, wsInfo.getProjectBin());
            }

            URLClassLoader urlClassLoader = new URLClassLoader(classPathURLs.toArray(new URL[classPathURLs.size()]),
                    Thread.currentThread().getContextClassLoader());

            deployment2ClassLoader.put(di.getName(), urlClassLoader);

            deployAdmin.deploy(di.getName(), urlClassLoader, serviceClasses);
        } catch (Exception e) {
            log.error("failed to deploy project " + di.getDeployID(), e);
        }
    }

    public void setDeployAdmin(DeploymentAdmin deployAdmin) {
        this.deployAdmin = deployAdmin;
    }

    public void setRulesProjectResolver(RulesProjectResolver rulesProjectResolver) {
        this.rulesProjectResolver = rulesProjectResolver;
    }

    public synchronized void undeploy(DeploymentInfo di) {
        if (deployment2ClassLoader.remove(di.getName()) != null) {
            deployAdmin.undeploy(di.getName());
        }
    }

}
