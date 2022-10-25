package org.openl.rules.project.model.v5_17;

import org.openl.rules.project.model.WildcardPattern;
import org.openl.rules.project.xml.XmlRulesDeploySerializer.MapAdapter;
import org.openl.rules.project.xml.v5_17.XmlRulesDescriptorSerializer_v5_17;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

import static org.openl.rules.project.xml.XmlRulesDeploySerializer.LAZY_MODULES_FOR_COMPILATION;
import static org.openl.rules.project.xml.XmlRulesDeploySerializer.MODULE_NAME;
import static org.openl.rules.project.xml.XmlRulesDeploySerializer.PUBLISHERS_TAG;
import static org.openl.rules.project.xml.XmlRulesDeploySerializer.PUBLISHER_TAG;
import static org.openl.rules.project.xml.XmlRulesDeploySerializer.RULES_DEPLOY_DESCRIPTOR_TAG;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=RULES_DEPLOY_DESCRIPTOR_TAG)
public class RulesDeploy_v5_17 {

    private Boolean isProvideRuntimeContext;
    private Boolean isProvideVariations;
    private Boolean useRuleServiceRuntimeContext;
    private String serviceName;
    @XmlElementWrapper(name = PUBLISHERS_TAG)
    @XmlElement(name = PUBLISHER_TAG)
    @XmlJavaTypeAdapter(XmlRulesDescriptorSerializer_v5_17.PublisherType_v5_17XmlAdapter.class)
    private PublisherType_v5_17[] publishers;
    private String interceptingTemplateClassName;
    private String annotationTemplateClassName;
    private String serviceClass;
    private String rmiServiceClass;
    private String url;
    private String version;
    private String groups;
    @XmlJavaTypeAdapter(MapAdapter.class)
    private Map<String, Object> configuration;
    @XmlElementWrapper(name = LAZY_MODULES_FOR_COMPILATION)
    @XmlElement(name = MODULE_NAME)
    private WildcardPattern[] lazyModulesForCompilationPatterns;

    public PublisherType_v5_17[] getPublishers() {
        return publishers;
    }

    public void setPublishers(PublisherType_v5_17[] publishers) {
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
