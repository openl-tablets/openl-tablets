package org.openl.rules.project.model;

import static org.openl.rules.project.xml.XmlRulesDeploySerializer.LAZY_MODULES_FOR_COMPILATION;
import static org.openl.rules.project.xml.XmlRulesDeploySerializer.MODULE_NAME;
import static org.openl.rules.project.xml.XmlRulesDeploySerializer.PUBLISHERS_TAG;
import static org.openl.rules.project.xml.XmlRulesDeploySerializer.PUBLISHER_TAG;
import static org.openl.rules.project.xml.XmlRulesDeploySerializer.RULES_DEPLOY_DESCRIPTOR_TAG;

import java.util.Map;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.rules.project.xml.XmlRulesDeploySerializer.MapAdapter;
import org.openl.rules.project.xml.XmlRulesDeploySerializer.PublisherTypeXmlAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = RULES_DEPLOY_DESCRIPTOR_TAG)
public class RulesDeploy {

    public enum PublisherType {
        WEBSERVICE,
        RESTFUL,
        RMI,
        KAFKA
    }

    private Boolean isProvideRuntimeContext;
    private Boolean isProvideVariations;
    private String serviceName;
    @XmlElementWrapper(name = PUBLISHERS_TAG)
    @XmlElement(name = PUBLISHER_TAG)
    @XmlJavaTypeAdapter(PublisherTypeXmlAdapter.class)
    private PublisherType[] publishers;
    private String interceptingTemplateClassName;
    private String annotationTemplateClassName;
    private String serviceClass;
    private String rmiServiceClass;
    private String url;
    private String rmiName;
    private String version;
    private String groups;
    @XmlJavaTypeAdapter(MapAdapter.class)
    private Map<String, Object> configuration;

    @XmlElementWrapper(name = LAZY_MODULES_FOR_COMPILATION)
    @XmlElement(name = MODULE_NAME)
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

    @Deprecated
    public WildcardPattern[] getLazyModulesForCompilationPatterns() {
        return lazyModulesForCompilationPatterns;
    }

    @Deprecated
    public void setLazyModulesForCompilationPatterns(WildcardPattern[] lazyModulesForCompilationPatterns) {
        this.lazyModulesForCompilationPatterns = lazyModulesForCompilationPatterns;
    }

    public String getGroups() {
        return groups;
    }

    public void setGroups(String groups) {
        this.groups = groups;
    }

    public String getRmiName() {
        return rmiName;
    }

    public void setRmiName(String rmiName) {
        this.rmiName = rmiName;
    }

}
