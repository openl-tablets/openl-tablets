package org.openl.rules.project.model.v5_15;

import java.util.Map;

public class RulesDeploy_v5_15 {

    public enum PublisherType {
        WEBSERVICE,
        RESTFUL
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
    private Boolean useRuleServiceRuntimeContext;
    private String serviceName;
    private PublisherType[] publishers;
    private String interceptingTemplateClassName;
    private String serviceClass;
    private String url;
    private Map<String, Object> configuration;

    private WildcardPattern[] lazyModulesForCompilationPatterns;

    public PublisherType[] getPublishers() {
        return publishers;
    }

    public void setPublishers(PublisherType[] publishers) {
        this.publishers = publishers;
    }

    public Boolean isProvideRuntimeContext() {
        return isProvideRuntimeContext;
    }

    public void setProvideRuntimeContext(Boolean isProvideRuntimeContext) {
        this.isProvideRuntimeContext = isProvideRuntimeContext;
    }

    public void setUseRuleServiceRuntimeContext(Boolean useRuleServiceRuntimeContext) {
        this.useRuleServiceRuntimeContext = useRuleServiceRuntimeContext;
    }

    public Boolean isUseRuleServiceRuntimeContext() {
        return useRuleServiceRuntimeContext;
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

    public WildcardPattern[] getLazyModulesForCompilationPatterns() {
        return lazyModulesForCompilationPatterns;
    }

    public void setLazyModulesForCompilationPatterns(WildcardPattern[] lazyModulesForCompilationPatterns) {
        this.lazyModulesForCompilationPatterns = lazyModulesForCompilationPatterns;
    }
}
