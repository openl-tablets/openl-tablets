package org.openl.rules.ruleservice.servlet;

import java.util.List;

/**
 * An interface for publisher that should be displayed on Servlet page.
 *
 * @author Nail Samatov
 *
 */
public interface AvailableServicesPresenter {
    List<ServiceInfo> getAvailableServices();
}
