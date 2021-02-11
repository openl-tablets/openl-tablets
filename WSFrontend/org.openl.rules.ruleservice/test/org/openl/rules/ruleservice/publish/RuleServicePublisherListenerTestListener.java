package org.openl.rules.ruleservice.publish;

import org.openl.rules.ruleservice.core.OpenLService;

public class RuleServicePublisherListenerTestListener implements RuleServicePublisherListener {

    public static volatile int onDeployCount = 0;
    public static volatile int onUndeployCount = 0;

    @Override
    public synchronized void onDeploy(OpenLService service) {
        onDeployCount++;
    }

    @Override
    public synchronized void onUndeploy(String deployPath) {
        onUndeployCount++;
    }
}
