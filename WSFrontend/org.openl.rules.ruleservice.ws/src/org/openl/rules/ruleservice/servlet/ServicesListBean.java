package org.openl.rules.ruleservice.servlet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.openl.rules.ruleservice.publish.MultipleRuleServicePublisher;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

@ManagedBean
@RequestScoped
public class ServicesListBean {
    @ManagedProperty(value = "#{ruleServicePublisher}")
    private RuleServicePublisher ruleServicePublisher;

    public void setRuleServicePublisher(RuleServicePublisher ruleServicePublisher) {
        this.ruleServicePublisher = ruleServicePublisher;
    }

    public List<AvailableServicesGroup> getServicesGroup() {
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

            for (RuleServicePublisher p : multiplePublisher.getSupportedPublishers()) {
                addServicesGroup(services, p);
            }
        }
    }

    public TreeNode getTree(ServiceInfo service) {
        BasicTreeNode root = new BasicTreeNode("Root", "root");
        if (service == null) {
            return root;
        }
        BasicTreeNode methodsRoot = new BasicTreeNode(service.getName(), "service");
        root.addChild(methodsRoot.getText(), methodsRoot);

        List<String> methodNames = service.getMethodNames();
        for (String methodName : methodNames) {
            methodsRoot.addChild(methodName, new BasicTreeNode(true, methodName, "method"));
        }

        return root;
    }

    public static class BasicTreeNode extends TreeNodeImpl {
        private final String text;
        private final String type;

        public BasicTreeNode(String text, String type) {
            this(false, text, type);
        }

        public BasicTreeNode(boolean leaf, String text, String type) {
            super(leaf);
            this.text = text;
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public String getType() {
            return type;
        }
    }
}
