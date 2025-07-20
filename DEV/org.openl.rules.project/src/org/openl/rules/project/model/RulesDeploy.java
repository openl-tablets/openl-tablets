package org.openl.rules.project.model;

import static org.openl.rules.project.xml.XmlRulesDeploySerializer.PUBLISHERS_TAG;
import static org.openl.rules.project.xml.XmlRulesDeploySerializer.PUBLISHER_TAG;
import static org.openl.rules.project.xml.XmlRulesDeploySerializer.RULES_DEPLOY_DESCRIPTOR_TAG;

import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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

    public String getAnnotationTemplateClassName() {
        return annotationTemplateClassName;
    }

    public void setAnnotationTemplateClassName(String annotationTemplateClassName) {
        this.annotationTemplateClassName = annotationTemplateClassName;
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
