package org.openl.rules.ruleservice.publish;

import org.openl.rules.ruleservice.core.OpenLService;

public class RuleServicePublisherLIstenerTestListener implements RuleServicePublisherListener {

    public static volatile int onDeployCount = 0;
    public static volatile int onUndeployCount = 0;

    @Override
    public void onDeploy(OpenLService service) {
        onDeployCount++;
    }

    @Override
    public void onUndeploy(String serviceName) {
        onUndeployCount++;
    }
}
