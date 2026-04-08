package org.openl.rules.ruleservice.core;

import lombok.Value;

import org.openl.rules.common.CommonVersion;

@Value
public class DeploymentDescription {
    String name;
    CommonVersion version;
}
