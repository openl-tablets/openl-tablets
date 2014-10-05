package org.openl.rules.ruleservice.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openl.rules.ruleservice.publish.MultipleRuleServicePublisher;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class ServicesListServlet extends HttpServlet {
    private static final long serialVersionUID = 714926204750487226L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RuleServicePublisher ruleServicePublisher = (RuleServicePublisher) WebApplicationContextUtils.getWebApplicationContext(request.getServletContext()).getBean("ruleServicePublisher");

        request.setAttribute("servicesGroup", getServicesGroup(ruleServicePublisher));

        RequestDispatcher rd = request.getRequestDispatcher("/services/index.jsp");
        rd.forward(request, response);
    }


    public List<AvailableServicesGroup> getServicesGroup(RuleServicePublisher ruleServicePublisher) {
        List<AvailableServicesGroup> services = new ArrayList<AvailableServicesGroup>();

        addServicesGroup(services, ruleServicePublisher);

        for (Iterator<AvailableServicesGroup> iterator = services.iterator(); iterator.hasNext(); ) {
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
            addServicesGroup(services, multiplePublisher.getDefaultRuleServicePublisher());

            for (RuleServicePublisher p : multiplePublisher.getSupportedPublishers().values()) {
                addServicesGroup(services, p);
            }
        }
    }
}
