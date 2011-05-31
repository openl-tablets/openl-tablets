package org.openl.ruleservice.management;

import java.util.List;

import org.openl.ruleservice.ServiceDescription;
import org.openl.ruleservice.loader.IRulesLoader;

public interface IServiceConfigurer {
    List<ServiceDescription> getServiceToBeDeployed(IRulesLoader loader);
}
