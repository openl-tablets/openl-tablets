package org.openl.rules.ruleservice.publish;

import org.openl.rules.ruleservice.core.OpenLService;

/**
 * Interface for publisher Listener
 *
 * @author Marat Kamalov.
 */
public interface RuleServicePublisherListener {

    void onDeploy(OpenLService service);

    void onUndeploy(String serviceName);
}
