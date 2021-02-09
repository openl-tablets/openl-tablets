package org.openl.rules.webstudio.web.repository;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.project.xml.SupportedVersion;

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
        rulesDeploy.setServiceName(serviceName);
    }

    public String getAnnotationTemplateClassName() {
        return rulesDeploy.getAnnotationTemplateClassName();
    }

    public void setAnnotationTemplateClassName(String annotationTemplateClassName) {
        rulesDeploy.setAnnotationTemplateClassName(annotationTemplateClassName);
    }

    public String getInterceptingTemplateClassName() {
        return rulesDeploy.getInterceptingTemplateClassName();
    }

    public void setInterceptingTemplateClassName(String interceptingTemplateClassName) {
        rulesDeploy.setInterceptingTemplateClassName(interceptingTemplateClassName);
    }

    public String getServiceClass() {
        return rulesDeploy.getServiceClass();
    }

    public void setServiceClass(String serviceClass) {
        rulesDeploy.setServiceClass(serviceClass);
    }

    public String getRmiServiceClass() {
        return rulesDeploy.getRmiServiceClass();
    }

    public void setRmiServiceClass(String rmiServiceClass) {
        rulesDeploy.setRmiServiceClass(rmiServiceClass);
    }

    public String getVersion() {
        return rulesDeploy.getVersion();
    }

    public void setVersion(String version) {
        rulesDeploy.setVersion(version);
    }

    public String getUrl() {
        return rulesDeploy.getUrl();
    }

    public void setUrl(String url) {
        rulesDeploy.setUrl(url);
    }

    public String getGroups() {
        return rulesDeploy.getGroups();
    }

    public void setGroups(String groups) {
        rulesDeploy.setGroups(groups);
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
