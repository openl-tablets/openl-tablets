package org.openl.rules.ruleservice.servlet.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openl.rules.ruleservice.publish.MultipleRuleServicePublisher;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.openl.rules.ruleservice.servlet.AvailableServicesPresenter;
import org.openl.rules.ruleservice.servlet.ServiceInfo;
import org.openl.rules.ruleservice.servlet.ServiceResource;
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
		request.setAttribute("services", getServicesGroup(ruleServicePublisher));
		return "index";
	}

	public List<ServiceInfo> getServicesGroup(RuleServicePublisher ruleServicePublisher) {
		List<AvailableServicesPresenter> services = new ArrayList<AvailableServicesPresenter>();

		addServicesGroup(services, ruleServicePublisher);

		for (Iterator<AvailableServicesPresenter> iterator = services.iterator(); iterator.hasNext();) {
			AvailableServicesPresenter servicesGroup = iterator.next();
			if (servicesGroup.getAvailableServices().isEmpty()) {
				iterator.remove();
			}
		}

		return mergeServicesServiceInfos(services);
	}

	private List<ServiceInfo> mergeServicesServiceInfos(List<AvailableServicesPresenter> services) {
		Map<String, List<ServiceInfo>> serviceInfos = new HashMap<String, List<ServiceInfo>>();
		for (AvailableServicesPresenter service : services) {
			for (ServiceInfo serviceInfo : service.getAvailableServices()) {
				List<ServiceInfo> list = serviceInfos.get(serviceInfo.getName());
				if (list == null) {
					list = new ArrayList<ServiceInfo>();
					serviceInfos.put(serviceInfo.getName(), list);
				}
				list.add(serviceInfo);
			}
		}
		List<ServiceInfo> ret = new ArrayList<ServiceInfo>();
		for (List<ServiceInfo> list : serviceInfos.values()) {
			ServiceInfo upToDateServiceInfo = null;
			List<ServiceResource> serviceResources = new ArrayList<ServiceResource>();
			for (ServiceInfo serviceInfo : list) {
				for (ServiceResource serviceUrlInfo : serviceInfo.getServiceResources()) {
					serviceResources.add(serviceUrlInfo);
				}
				if (upToDateServiceInfo == null) {
					upToDateServiceInfo = serviceInfo;
				} else {
					if (upToDateServiceInfo.getStartedTime().before(serviceInfo.getStartedTime())) {
						upToDateServiceInfo = serviceInfo;
					}
				}
			}
			ret.add(new ServiceInfo(upToDateServiceInfo.getStartedTime(), upToDateServiceInfo.getName(),
					upToDateServiceInfo.getMethodNames(), serviceResources.toArray(new ServiceResource[] {})));
		}
		return ret;
	}

	private void addServicesGroup(List<AvailableServicesPresenter> services, RuleServicePublisher publisher) {
		if (publisher instanceof AvailableServicesPresenter) {
			if (services.contains(publisher)) {
				return;
			}
			services.add((AvailableServicesPresenter) publisher);
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
