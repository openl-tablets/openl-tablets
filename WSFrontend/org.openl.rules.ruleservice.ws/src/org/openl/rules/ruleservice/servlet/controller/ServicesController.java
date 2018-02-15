package org.openl.rules.ruleservice.servlet.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openl.rules.ruleservice.publish.MultipleRuleServicePublisher;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.openl.rules.ruleservice.servlet.AvailableServicesPresenter;
import org.openl.rules.ruleservice.servlet.ServiceInfo;
import org.openl.rules.ruleservice.servlet.ServiceResource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServicesController {

    public static String getServices(HttpServletRequest request) throws JsonProcessingException {
        WebApplicationContext context = WebApplicationContextUtils
            .getWebApplicationContext(request.getServletContext());
        RuleServicePublisher ruleServicePublisher = context.getBean("ruleServicePublisher", RuleServicePublisher.class);
        Collection<ServiceInfo> serviceInfos = getServiceInfos(ruleServicePublisher);
        String json = new ObjectMapper().writeValueAsString(serviceInfos);
        return json;
    }

    private static Collection<ServiceInfo> getServiceInfos(RuleServicePublisher publisher) {
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
                collectServiceInfos(serviceInfos, p);
            }
        } else {
            // Or single service publisher
            collectServiceInfos(serviceInfos, publisher);
        }

        return serviceInfos.values();
    }

    private static void collectServiceInfos(Map<String, ServiceInfo> serviceInfos, RuleServicePublisher publisher) {
        if (publisher instanceof AvailableServicesPresenter) {
            List<ServiceInfo> services = ((AvailableServicesPresenter) publisher).getAvailableServices();
            for (ServiceInfo serviceInfo : services) {
                String serviceName = serviceInfo.getName();
                ServiceInfo current = serviceInfos.get(serviceName);
                if (current == null) {
                    serviceInfos.put(serviceName, serviceInfo);
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
                    serviceInfos.put(serviceName, newServiceInfo);
                }
            }
        }
    }
}
