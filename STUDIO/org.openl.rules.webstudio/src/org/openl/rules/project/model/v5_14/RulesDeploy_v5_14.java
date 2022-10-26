package org.openl.rules.project.model.v5_14;

import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.project.xml.v5_14.PublisherTypeXmlAdapter_v5_14;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

import static org.openl.rules.project.xml.XmlRulesDeploySerializer.PUBLISHERS_TAG;
import static org.openl.rules.project.xml.XmlRulesDeploySerializer.PUBLISHER_TAG;
import static org.openl.rules.project.xml.XmlRulesDeploySerializer.RULES_DEPLOY_DESCRIPTOR_TAG;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=RULES_DEPLOY_DESCRIPTOR_TAG)
public class RulesDeploy_v5_14 {

    private Boolean isProvideRuntimeContext;
    private Boolean isProvideVariations;
    private Boolean useRuleServiceRuntimeContext;
    private String serviceName;
    @XmlElementWrapper(name = PUBLISHERS_TAG)
    @XmlElement(name = PUBLISHER_TAG)
    @XmlJavaTypeAdapter(PublisherTypeXmlAdapter_v5_14.class)
    private PublisherType_v5_14[] publishers;
    private String interceptingTemplateClassName;
    private String serviceClass;
    private String url;
    @XmlJavaTypeAdapter(XmlRulesDeploySerializer.MapAdapter.class)
    private Map<String, Object> configuration;

    public PublisherType_v5_14[] getPublishers() {
        return publishers;
    }

    public void setPublishers(PublisherType_v5_14[] publishers) {
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
}
