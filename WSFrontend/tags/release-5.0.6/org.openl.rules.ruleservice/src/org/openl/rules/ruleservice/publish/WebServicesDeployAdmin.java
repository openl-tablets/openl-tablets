package org.openl.rules.ruleservice.publish;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.openl.rules.ruleservice.helper.CglibInstantiationStrategy;
import org.openl.rules.ruleservice.helper.InstantiationStrategy;
import org.openl.rules.ruleservice.helper.EngineFactoryInstantiationStrategy;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class WebServicesDeployAdmin implements DeployAdmin {
    private static final Log log = LogFactory.getLog(WebServicesDeployAdmin.class);

    private Map<String, Collection<Server>> runningServices = new HashMap<String, Collection<Server>>();

    public synchronized void deploy(String serviceName, ClassLoader loader, List<WSInfo> infoList) {
        undeploy(serviceName);

        String address = "http://localhost:9000/" + serviceName + "/";

        Collection<Server> servers = new ArrayList<Server>();
        for (WSInfo wsInfo : infoList) {
            try {
                servers.add(deploy(address, loader, wsInfo));
            } catch (Exception e) {
                log.error("failed to create service", e);
            }
        }

        runningServices.put(serviceName, servers);
    }

    public synchronized void undeploy(String serviceName) {
        Collection<Server> servers = runningServices.remove(serviceName);
        if (servers != null) {
            for (Server server : servers) {
                server.stop();
            }
        }
    }

    private Server deploy(String baseAddress, ClassLoader loader, WSInfo wsInfo)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<?> aClass = loader.loadClass(wsInfo.getClassName());

        ServerFactoryBean svrFactory = new ServerFactoryBean();
        svrFactory.setServiceClass(aClass);
        svrFactory.setAddress(baseAddress + wsInfo.getName());
        svrFactory.getServiceFactory().setDataBinding(new AegisDatabinding());

        svrFactory.setServiceBean(getStrategy(wsInfo).instantiate(aClass));

        return svrFactory.create();
    }

    private InstantiationStrategy getStrategy(WSInfo wsInfo) {
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
}
