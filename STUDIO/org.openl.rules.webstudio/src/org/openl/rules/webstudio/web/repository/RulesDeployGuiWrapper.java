package org.openl.rules.webstudio.web.repository;

import java.util.Optional;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.project.xml.SupportedVersion;
import org.openl.util.StringUtils;

public class RulesDeployGuiWrapper {
    private static final PublisherType[] DEFAULT_PUBLISHERS = new PublisherType[] { PublisherType.RESTFUL };

    private final RulesDeploy rulesDeploy;
    private String configuration;
    private final SupportedVersion version;

    public RulesDeployGuiWrapper(RulesDeploy rulesDeploy, SupportedVersion version) {
        this.rulesDeploy = rulesDeploy;
        this.version = version;
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
        rulesDeploy.setServiceName(StringUtils.trimToNull(serviceName));
    }

    @Deprecated
    public String getInterceptingTemplateClassName() {
        return rulesDeploy.getInterceptingTemplateClassName();
    }

    @Deprecated
    public void setInterceptingTemplateClassName(String interceptingTemplateClassName) {
        rulesDeploy.setInterceptingTemplateClassName(StringUtils.trimToNull(interceptingTemplateClassName));
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

    public String getRmiServiceClass() {
        return rulesDeploy.getRmiServiceClass();
    }

    public void setRmiServiceClass(String rmiServiceClass) {
        rulesDeploy.setRmiServiceClass(StringUtils.trimToNull(rmiServiceClass));
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
        PublisherType[] publishers = rulesDeploy.getPublishers();
        if (publishers == null || publishers.length == 0) {
            // Set both services by default
            publishers = DEFAULT_PUBLISHERS;
        }
        return publishers;
    }

    public PublisherType[] getAvailablePublishers() {
        if (version.compareTo(SupportedVersion.V5_15) <= 0) {
            return new PublisherType[] { PublisherType.WEBSERVICE, PublisherType.RESTFUL };
        }

        if (version.compareTo(SupportedVersion.V5_22) <= 0) {
            return new PublisherType[] { PublisherType.WEBSERVICE, PublisherType.RESTFUL, PublisherType.RMI };
        }

        return PublisherType.values();
    }

}
