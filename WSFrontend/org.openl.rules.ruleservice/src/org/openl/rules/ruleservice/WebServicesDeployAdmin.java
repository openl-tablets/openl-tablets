package org.openl.rules.ruleservice;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.frontend.ServerFactoryBean;

import java.util.List;

public class WebServicesDeployAdmin {

    public static void deploy(String serviceName, ClassLoader loader, List<String> serviceClasses) {
        String address = "http://localhost:9000/" + serviceName + "/";

        for (String className : serviceClasses) {
            try {
                deploy(address, loader, className);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void deploy(String baseAddress, ClassLoader loader, String className)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<?> aClass = loader.loadClass(className);

        ServerFactoryBean svrFactory = new ServerFactoryBean();
        svrFactory.setServiceClass(aClass);
        svrFactory.setAddress(baseAddress + className);
        svrFactory.getServiceFactory().setDataBinding(new AegisDatabinding());

        svrFactory.setServiceBean(aClass.newInstance());

        svrFactory.create();
    }
}
