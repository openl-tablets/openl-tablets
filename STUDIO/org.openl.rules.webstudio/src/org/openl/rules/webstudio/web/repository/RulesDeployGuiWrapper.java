package org.openl.rules.webstudio.web.repository;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.RulesDeploy.PublisherType;

import javax.validation.constraints.Size;

public class RulesDeployGuiWrapper {
    private final RulesDeploy rulesDeploy;
    private String configuration;

    public RulesDeployGuiWrapper(RulesDeploy rulesDeploy) {
        this.rulesDeploy = rulesDeploy;
    }

    public RulesDeploy getRulesDeploy() {
        return rulesDeploy;
    }

    public boolean isProvideRuntimeContext() {
        Boolean provideRuntimeContext = rulesDeploy.isProvideRuntimeContext();
        return provideRuntimeContext != null ? provideRuntimeContext : false;
    }

    public void setProvideRuntimeContext(boolean provideRuntimeContext) {
        rulesDeploy.setProvideRuntimeContext(provideRuntimeContext);
    }

    public boolean isProvideVariations() {
        Boolean provideVariations = rulesDeploy.isProvideVariations();
        return provideVariations != null ? provideVariations : false;
    }

    public void setProvideVariations(boolean provideRuntimeContext) {
        rulesDeploy.setProvideVariations(provideRuntimeContext);
    }

    public String getServiceName() {
        return rulesDeploy.getServiceName();
    }

    public void setServiceName(String serviceName) {
        rulesDeploy.setServiceName(serviceName);
    }

    public String getServiceClass() {
        return rulesDeploy.getServiceClass();
    }

    public void setServiceClass(String serviceClass) {
        rulesDeploy.setServiceClass(serviceClass);
    }

    public String getUrl() {
        return rulesDeploy.getUrl();
    }

    public void setUrl(String url) {
        rulesDeploy.setUrl(url);
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public void setPublishers(PublisherType[] publishers) {
        rulesDeploy.setPublishers(publishers);
    }

    public PublisherType[] getPublishers() {
        return rulesDeploy.getPublishers();
    }

    public PublisherType[] getAvailablePublishers() {
        return PublisherType.values();
    }
}
