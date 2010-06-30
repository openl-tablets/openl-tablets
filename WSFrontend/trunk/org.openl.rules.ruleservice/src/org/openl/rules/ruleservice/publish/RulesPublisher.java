package org.openl.rules.ruleservice.publish;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.ruleservice.loader.DeploymentInfo;

import java.io.File;
import java.util.List;

public class RulesPublisher {
    private final Log log = LogFactory.getLog(getClass());

    private DeploymentAdmin deployAdmin;
    private RulesProjectResolver rulesProjectResolver;
    private ServiceNameBuilder serviceNameBuilder;
    
    public RulesPublisher(){
        rulesProjectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
    }
    
    private List<ProjectDescriptor> resolveProjects(File deploymentLocalFolder) {
        rulesProjectResolver.setWorkspace(deploymentLocalFolder.getAbsolutePath());
        return rulesProjectResolver.listOpenLProjects();
    }

    public synchronized void deploy(DeploymentInfo di, File deploymentLocalFolder) {
        try {
            List<ProjectDescriptor> serviceClasses = resolveProjects(deploymentLocalFolder);
            String serviceName = serviceNameBuilder.getServiceName(di);
            
            deployAdmin.deploy(serviceName, serviceClasses);
        } catch (Exception e) {
            log.error(String.format("Failed to deploy project \"%s\"", di.getDeployID()), e);
        }
    }

    public synchronized void undeploy(DeploymentInfo di) {
        deployAdmin.undeploy(di.getName());
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
