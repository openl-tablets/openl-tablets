package org.openl.ruleservice.simple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.ruleservice.OpenLService;
import org.openl.ruleservice.ServiceDeployException;
import org.openl.ruleservice.publish.IDeploymentAdmin;

public class JavaClassDeploymentAdmin implements IDeploymentAdmin {

    private RulesFrontend frontend;
    private Map<String, OpenLService> runningServices = new HashMap<String, OpenLService>();

    public RulesFrontend getFrontend() {
        return frontend;
    }

    public void setFrontend(RulesFrontend frontend) {
        this.frontend = frontend;
    }

    public OpenLService deploy(OpenLService service) throws ServiceDeployException {
        frontend.registerService(service);
        return runningServices.put(service.getName(), service);
    }

    public OpenLService undeploy(String serviceName) throws ServiceDeployException {
        frontend.unregisterService(serviceName);
        return runningServices.remove(serviceName);
    }

    public List<OpenLService> getRunningServices() {
        return new ArrayList<OpenLService>(runningServices.values());
    }

    public OpenLService findServiceByName(String name) {
        return runningServices.get(name);
    }

}
