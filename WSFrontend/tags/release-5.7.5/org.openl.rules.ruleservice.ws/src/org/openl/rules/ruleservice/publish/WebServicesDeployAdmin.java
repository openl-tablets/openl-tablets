package org.openl.rules.ruleservice.publish;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.rules.project.dependencies.RulesModuleDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.instantiation.RulesServiceEnhancer;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.springframework.beans.factory.ObjectFactory;

public class WebServicesDeployAdmin implements DeploymentAdmin {
    
    private static final Log LOG = LogFactory.getLog(WebServicesDeployAdmin.class);
    
    private ObjectFactory<?> serverFactory;
    private Map<String, Collection<Server>> runningServices = new HashMap<String, Collection<Server>>();
    private String baseAddress;
    private RulesServiceEnhancer serviceEnhancer;
    private boolean provideRuntimeContext;    

    public String getBaseAddress() {
        return baseAddress;
    }

    public void setBaseAddress(String address) {
        this.baseAddress = address;
    }

    public ObjectFactory<?> getServerFactory() {
        return serverFactory;
    }

    public void setServerFactory(ObjectFactory<?> serverFactory) {
        this.serverFactory = serverFactory;
    }

    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    public void setProvideRuntimeContext(boolean provideRuntimeContext) {
        this.provideRuntimeContext = provideRuntimeContext;
    }

    public synchronized void deploy(String serviceName, List<ProjectDescriptor> infoList) {
        undeploy(serviceName);

        String address = getBaseAddress() + serviceName + "/";

        Collection<Server> servers = new ArrayList<Server>();
        for (ProjectDescriptor wsInfo : infoList) {
            for (Module rulesModule : wsInfo.getModules()) {
                try {
                    servers.add(deploy(address, rulesModule, infoList));
                } catch (Throwable t) {
                    LOG.error("Failed to create service", t);
                }
            }
        }

        runningServices.put(serviceName, servers);
    }

    private Server deploy(String baseAddress, Module rulesModule, List<ProjectDescriptor> infoList)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        ServerFactoryBean svrFactory = getServerFactoryBean();
        
        instantiateServiceBean(rulesModule, svrFactory, infoList);
       
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
 
    @SuppressWarnings("deprecation")
    private void instantiateServiceBean(Module rulesModule, ServerFactoryBean svrFactory, List<ProjectDescriptor> infoList)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        IDependencyManager dependencyManager = initDependencyManager(infoList);
        
        RulesInstantiationStrategy strategy = RulesInstantiationStrategyFactory.getStrategy(rulesModule, true, dependencyManager);
        
        if (isProvideRuntimeContext()) {
            serviceEnhancer = new RulesServiceEnhancer(strategy);         
            
            svrFactory.setServiceClass(serviceEnhancer.getServiceClass());
            svrFactory.setServiceBean(serviceEnhancer.instantiate(ReloadType.SINGLE));            
        } else {
            svrFactory.setServiceClass(strategy.getServiceClass());
            svrFactory.setServiceBean(strategy.instantiate(ReloadType.SINGLE));
        }            
    }
    
    private IDependencyManager initDependencyManager(List<ProjectDescriptor> infoList) {
        List<Module> modules = new ArrayList<Module>();
        for (ProjectDescriptor wsInfo : infoList) {
            modules.addAll(wsInfo.getModules());
        }
        RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();
        
        dependencyManager.setExecutionMode(true);
        IDependencyLoader loader1 = new RulesModuleDependencyLoader(modules);
        
        dependencyManager.setDependencyLoaders(Arrays.asList(loader1));
        return dependencyManager;
    }

    protected String getServiceNameForModule(Module rulesModule) {
        int postfixIndex = rulesModule.getClassname().lastIndexOf("Wrapper");
        if (postfixIndex > 0) {
            return rulesModule.getClassname().substring(0, postfixIndex);
        } else {
            return rulesModule.getClassname();
        }
    }
    
    private Server exposeWebService(String baseAddress, Module rulesModule, ServerFactoryBean svrFactory) {
        svrFactory.setAddress(baseAddress + getServiceNameForModule(rulesModule));
        Server wsServer =  null;
        
        // Set classLoader used for whole compilation of Openl rules and loading service class of Openl
        // as current one. It is done to give access to datatypes generated and loaded to classloader at runtime.
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();                
        Thread.currentThread().setContextClassLoader(svrFactory.getServiceClass().getClassLoader());
        try {
            wsServer =  svrFactory.create();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        
        return wsServer;
    }
}
