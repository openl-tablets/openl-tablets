package org.openl.rules.ruleservice.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.openl.rules.ruleservice.publish.MultipleRuleServicePublisher;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;

public class PublisherUtils {

    public static Collection<ServiceInfo> getServicesInfo(RuleServicePublisher publisher) {
        Map<String, ServiceInfo> serviceInfos = new HashMap<>();

        if (publisher instanceof MultipleRuleServicePublisher) {
            // Wrapped into collection of publishers
            MultipleRuleServicePublisher multiplePublisher = (MultipleRuleServicePublisher) publisher;

            Collection<RuleServicePublisher> defaultPublishers = multiplePublisher.getDefaultRuleServicePublishers();
            HashSet<RuleServicePublisher> publishers = new HashSet<>(defaultPublishers);

            Map<String, RuleServicePublisher> supportedPublishers = multiplePublisher.getSupportedPublishers();
            if (supportedPublishers != null) {
                publishers.addAll(supportedPublishers.values());
            }

            for (RuleServicePublisher p : publishers) {
                collectServicesInfo(serviceInfos, p);
            }
        } else {
            // Or single service publisher
            collectServicesInfo(serviceInfos, publisher);
        }

        return serviceInfos.values();
    }

    private static void collectServicesInfo(Map<String, ServiceInfo> servicesInfo, RuleServicePublisher publisher) {
        if (publisher instanceof AvailableServicesPresenter) {
            List<ServiceInfo> services = ((AvailableServicesPresenter) publisher).getAvailableServices();
            for (ServiceInfo serviceInfo : services) {
                String serviceName = serviceInfo.getName();
                ServiceInfo current = servicesInfo.get(serviceName);
                if (current == null) {
                    servicesInfo.put(serviceName, serviceInfo);
                } else {
                    // Join Resources
                    List<ServiceResource> res1 = Arrays.asList(current.getServiceResources());
                    List<ServiceResource> res2 = Arrays.asList(serviceInfo.getServiceResources());
                    List<ServiceResource> serviceResources = new ArrayList<>(res1);
                    serviceResources.addAll(res2);
                    ServiceResource[] resTotal = serviceResources.toArray(new ServiceResource[] {});

                    // Select the latest time
                    Date startedTime = current.getStartedTime();
                    Date newStartedTime = serviceInfo.getStartedTime();
                    if (startedTime.before(newStartedTime)) {
                        startedTime = newStartedTime;
                    }

                    // Methods names
                    List<String> methodNames = current.getMethodNames();

                    ServiceInfo newServiceInfo = new ServiceInfo(startedTime, serviceName, methodNames, resTotal);
                    servicesInfo.put(serviceName, newServiceInfo);
                }
            }
        }
    }
}
