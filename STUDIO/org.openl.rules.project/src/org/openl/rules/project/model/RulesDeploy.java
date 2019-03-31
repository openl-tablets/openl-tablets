package org.openl.rules.project.model;

import java.util.Map;

public class RulesDeploy {

    public enum PublisherType {
        WEBSERVICE,
        RESTFUL,
        RMI
    }

    public static class WildcardPattern {
        String value;

        public WildcardPattern(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private Boolean isProvideRuntimeContext;
    private Boolean isProvideVariations;
    private String serviceName;
    private PublisherType[] publishers;
    private String interceptingTemplateClassName;
    private String annotationTemplateClassName;
    private String serviceClass;
    private String rmiServiceClass;
    private String url;
    private String version;
    private String groups;
    private Map<String, Object> configuration;

    private WildcardPattern[] lazyModulesForCompilationPatterns;

    public PublisherType[] getPublishers() {
        return publishers;
    }

    public void setPublishers(PublisherType[] publishers) {
        this.publishers = publishers;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean isProvideRuntimeContext() {
        return isProvideRuntimeContext;
    }

    public void setProvideRuntimeContext(Boolean isProvideRuntimeContext) {
        this.isProvideRuntimeContext = isProvideRuntimeContext;
    }

    public Boolean isProvideVariations() {
        return isProvideVariations;
    }

    public void setProvideVariations(Boolean isProvideVariations) {
        this.isProvideVariations = isProvideVariations;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getRmiServiceClass() {
        return rmiServiceClass;
    }

    public void setRmiServiceClass(String rmiServiceClass) {
        this.rmiServiceClass = rmiServiceClass;
    }

    public String getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(String serviceClass) {
        this.serviceClass = serviceClass;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public String getInterceptingTemplateClassName() {
        return interceptingTemplateClassName;
    }

    public void setInterceptingTemplateClassName(String interceptingTemplateClassName) {
        this.interceptingTemplateClassName = interceptingTemplateClassName;
    }

    public String getAnnotationTemplateClassName() {
        return annotationTemplateClassName;
    }

    public void setAnnotationTemplateClassName(String annotationTemplateClassName) {
        this.annotationTemplateClassName = annotationTemplateClassName;
    }

    public WildcardPattern[] getLazyModulesForCompilationPatterns() {
        return lazyModulesForCompilationPatterns;
    }

    public void setLazyModulesForCompilationPatterns(WildcardPattern[] lazyModulesForCompilationPatterns) {
        this.lazyModulesForCompilationPatterns = lazyModulesForCompilationPatterns;
    }

    public String getGroups() {
        return groups;
    }

    public void setGroups(String groups) {
        this.groups = groups;
    }

}
