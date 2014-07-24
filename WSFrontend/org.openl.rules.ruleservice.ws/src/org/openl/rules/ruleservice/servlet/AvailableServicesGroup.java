package org.openl.rules.ruleservice.servlet;

import java.util.List;

public interface AvailableServicesGroup {
    String getGroupName();
    List<ServiceInfo> getAvailableServices();
}
