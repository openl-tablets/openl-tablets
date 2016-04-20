package org.openl.rules.ruleservice.servlet.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openl.rules.ruleservice.publish.MultipleRuleServicePublisher;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.openl.rules.ruleservice.servlet.AvailableServicesGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class ServicesController {

    @Autowired
    @Qualifier("ruleServicePublisher")
    RuleServicePublisher ruleServicePublisher;

    @RequestMapping(value = { "/services.form", "/" }, method = RequestMethod.GET)
    public String doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("servicesGroup", getServicesGroup(ruleServicePublisher));
        return "index";
    }

    public List<AvailableServicesGroup> getServicesGroup(RuleServicePublisher ruleServicePublisher) {
        List<AvailableServicesGroup> services = new ArrayList<AvailableServicesGroup>();

        addServicesGroup(services, ruleServicePublisher);

        for (Iterator<AvailableServicesGroup> iterator = services.iterator(); iterator.hasNext();) {
            AvailableServicesGroup servicesGroup = iterator.next();
            if (servicesGroup.getAvailableServices().isEmpty()) {
                iterator.remove();
            }
        }

        return services;
    }

    private void addServicesGroup(List<AvailableServicesGroup> services, RuleServicePublisher publisher) {
        if (publisher instanceof AvailableServicesGroup) {
            if (services.contains(publisher)) {
                return;
            }
            services.add((AvailableServicesGroup) publisher);
        }

        if (publisher instanceof MultipleRuleServicePublisher) {
            MultipleRuleServicePublisher multiplePublisher = (MultipleRuleServicePublisher) publisher;

            for (RuleServicePublisher p : multiplePublisher.getDefaultRuleServicePublishers()) {
                addServicesGroup(services, p);
            }

            for (RuleServicePublisher p : multiplePublisher.getSupportedPublishers().values()) {
                addServicesGroup(services, p);
            }
        }
    }
}
