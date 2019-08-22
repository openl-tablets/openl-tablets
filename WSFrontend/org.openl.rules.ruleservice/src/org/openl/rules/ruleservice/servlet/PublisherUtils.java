package org.openl.rules.ruleservice.servlet;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.publish.MultipleRuleServicePublisher;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;

public final class PublisherUtils {

    private PublisherUtils() {
    }

    public static Collection<ServiceInfo> getServicesInfo(RuleServicePublisher publisher) {
        Map<String, ServiceInfo> serviceInfos = new TreeMap<>();
        HashSet<RuleServicePublisher> publishers = getPublishers(publisher);
        for (RuleServicePublisher p : publishers) {
            collectServicesInfo(serviceInfos, p);
        }
        return serviceInfos.values();
    }

    public static List<ServiceMethodsInfo> getServiceMethods(RuleServicePublisher publisher,
            String serviceName) throws RuleServiceInstantiationException {
        OpenLService service = publisher.getServiceByName(serviceName);
        if (Objects.isNull(service)) {
            return new ArrayList<>();
        }
        List<Method> methods = Arrays.stream(service.getServiceClass().getMethods())
            .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
            .collect(Collectors.toList());
        List<ServiceMethodsInfo> serviceMethodsInfos = new ArrayList<>();
        for (Method method : methods) {
            List<String> params = Arrays.stream(method.getParameters())
                .map(m -> m.getType().getSimpleName())
                .collect(Collectors.toList());
            serviceMethodsInfos
                .add(new ServiceMethodsInfo(method.getName(), method.getReturnType().getSimpleName(), params));
        }
        return serviceMethodsInfos;
    }

    private static HashSet<RuleServicePublisher> getPublishers(RuleServicePublisher publisher) {
        if (publisher instanceof MultipleRuleServicePublisher) {
            // Wrapped into collection of publishers
            MultipleRuleServicePublisher multiplePublisher = (MultipleRuleServicePublisher) publisher;

            Collection<RuleServicePublisher> defaultPublishers = multiplePublisher.getDefaultRuleServicePublishers();
            HashSet<RuleServicePublisher> publishers = new HashSet<>(defaultPublishers);

            Map<String, RuleServicePublisher> supportedPublishers = multiplePublisher.getSupportedPublishers();
            if (supportedPublishers != null) {
                publishers.addAll(supportedPublishers.values());
            }
            return publishers;
        } else {
            // Or single service publisher
            HashSet<RuleServicePublisher> onePublisher = new HashSet();
            onePublisher.add(publisher);
            return onePublisher;
        }
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
                    // Join urls
                    Map<String, String> urls = new TreeMap<>(current.getUrls());
                    urls.putAll(serviceInfo.getUrls());

                    // Select the latest time
                    Date startedTime = current.getStartedTime();
                    Date newStartedTime = serviceInfo.getStartedTime();
                    if (startedTime.before(newStartedTime)) {
                        startedTime = newStartedTime;
                    }

                    ServiceInfo newServiceInfo = new ServiceInfo(startedTime, serviceName, urls);
                    servicesInfo.put(serviceName, newServiceInfo);
                }
            }
        }
    }
}
