package org.openl.ruleservice;

import java.util.List;

import org.openl.ruleservice.loader.IRulesLoader;
import org.openl.ruleservice.publish.IRulesPublisher;

public class RuleService {
    private IRulesLoader loader;
    private IRulesPublisher publisher;
    
    protected OpenLService createService(ServiceDescription serviceDescription) {
        //TODO
        return null;
    }
    
    public OpenLService deploy(ServiceDescription serviceDescription) throws ServiceDeployException {
        //TODO
        return null;
    }
    
    public OpenLService redeploy(ServiceDescription serviceDescription) throws ServiceDeployException {
        //TODO
        return null;
    }

    public OpenLService undeploy(String serviceName) throws ServiceDeployException {
        //TODO
        return null;
    }
    
    public List<OpenLService> getRunningServices(){
        return publisher.getRunningServices();
    }
}
