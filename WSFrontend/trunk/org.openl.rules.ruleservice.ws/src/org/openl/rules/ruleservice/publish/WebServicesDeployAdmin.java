package org.openl.rules.ruleservice.publish;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.openl.rules.ruleservice.instantiation.RulesInstantiationStrategy;
import org.openl.rules.ruleservice.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.ruleservice.resolver.RulesModuleInfo;
import org.openl.rules.ruleservice.resolver.RulesProjectInfo;
import org.springframework.beans.factory.ObjectFactory;

public class WebServicesDeployAdmin implements DeploymentAdmin {
    private static final Log LOG = LogFactory.getLog(WebServicesDeployAdmin.class);
    private ObjectFactory serverFactory;

    private Map<String, Collection<Server>> runningServices = new HashMap<String, Collection<Server>>();

    private String baseAddress;;

    public String getBaseAddress() {
        return baseAddress;
    }

    public void setBaseAddress(String address) {
        this.baseAddress = address;
    }

    public ObjectFactory getServerFactory() {
        return serverFactory;
    }

    public void setServerFactory(ObjectFactory serverFactory) {
        this.serverFactory = serverFactory;
    }

    public synchronized void deploy(String serviceName, ClassLoader loader, List<RulesProjectInfo> infoList) {
        undeploy(serviceName);

        String address = getBaseAddress() + serviceName + "/";

        Collection<Server> servers = new ArrayList<Server>();
        for (RulesProjectInfo wsInfo : infoList) {
            for (RulesModuleInfo rulesModule : wsInfo.getRulesModules()) {
                try {
                    servers.add(deploy(address, loader, rulesModule));
                } catch (Exception e) {
                    LOG.error("Failed to create service", e);
                }
            }
        }

        runningServices.put(serviceName, servers);
    }

    private Server deploy(String baseAddress, ClassLoader loader, RulesModuleInfo rulesModule)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        ServerFactoryBean svrFactory = getServerFactoryBean();

        instantiateServiceBean(loader, rulesModule, svrFactory);

        return exposeWebService(baseAddress, rulesModule, svrFactory);
    }
    
    public synchronized void undeploy(String serviceName) {
        Collection<Server> servers = runningServices.remove(serviceName);
        if (servers != null) {
            for (Server server : servers) {
                server.stop();
            }
        }
    }
    
    /*internal for test*/ ServerFactoryBean getServerFactoryBean() {
        if (serverFactory != null) {
            return (ServerFactoryBean) serverFactory.getObject();
        }
        return new ServerFactoryBean();
    }
 
    private void instantiateServiceBean(ClassLoader loader, RulesModuleInfo rulesModule, ServerFactoryBean svrFactory)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        RulesInstantiationStrategy strategy = RulesInstantiationStrategyFactory.getStrategy(rulesModule, loader);

        svrFactory.setServiceClass(strategy.getServiceClass());
        svrFactory.setServiceBean(strategy.instantiate());
    }
    
    private Server exposeWebService(String baseAddress, RulesModuleInfo rulesModule, ServerFactoryBean svrFactory) {
        svrFactory.setAddress(baseAddress + rulesModule.getName());
        return svrFactory.create();
    }
}
