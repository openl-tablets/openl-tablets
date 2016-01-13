package org.openl.rules.ruleservice.publish;

import org.openl.rules.ruleservice.core.OpenLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerRuleServicePublisherListener implements RuleServicePublisherListener{
    
    Logger log = LoggerFactory.getLogger(LoggerRuleServicePublisherListener.class);
    
    @Override
    public void onDeploy(OpenLService service) {
        log.info("Service '" + service.getName() + "' was deployed.");
    }
    
    @Override
    public void onUndeploy(String serviceName) {
        log.info("Service '" + serviceName + "' was undeployed.");
    }
}
