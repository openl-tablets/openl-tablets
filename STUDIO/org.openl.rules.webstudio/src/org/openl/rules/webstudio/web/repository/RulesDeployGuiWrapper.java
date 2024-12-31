package org.openl.rules.webstudio.web.repository;

import java.util.Optional;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.util.StringUtils;

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
        return Boolean.TRUE.equals(rulesDeploy.isProvideRuntimeContext());
    }

    public void setProvideRuntimeContext(boolean provideRuntimeContext) {
        rulesDeploy.setProvideRuntimeContext(provideRuntimeContext);
    }

    public String getServiceName() {
        return rulesDeploy.getServiceName();
    }

    public void setServiceName(String serviceName) {
        rulesDeploy.setServiceName(StringUtils.trimToNull(serviceName));
    }

    public void setTemplateClassName(String templateClassName) {
        rulesDeploy.setInterceptingTemplateClassName(null);
        rulesDeploy.setAnnotationTemplateClassName(StringUtils.trimToNull(templateClassName));
    }

    public String getTemplateClassName() {
        return Optional.ofNullable(rulesDeploy.getAnnotationTemplateClassName())
                .filter(StringUtils::isNotBlank)
                .orElseGet(rulesDeploy::getInterceptingTemplateClassName);
    }

    public String getServiceClass() {
        return rulesDeploy.getServiceClass();
    }

    public void setServiceClass(String serviceClass) {
        rulesDeploy.setServiceClass(StringUtils.trimToNull(serviceClass));
    }

    public String getVersion() {
        return rulesDeploy.getVersion();
    }

    public void setVersion(String version) {
        rulesDeploy.setVersion(StringUtils.trimToNull(version));
    }

    public String getUrl() {
        return rulesDeploy.getUrl();
    }

    public void setUrl(String url) {
        rulesDeploy.setUrl(StringUtils.trimToNull(url));
    }

    public String getGroups() {
        return rulesDeploy.getGroups();
    }

    public void setGroups(String groups) {
        rulesDeploy.setGroups(StringUtils.trimToNull(groups));
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
        return new PublisherType[]{PublisherType.RESTFUL, PublisherType.KAFKA};
    }

}
