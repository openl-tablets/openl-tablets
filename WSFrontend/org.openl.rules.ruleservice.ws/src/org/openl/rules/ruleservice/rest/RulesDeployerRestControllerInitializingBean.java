package org.openl.rules.ruleservice.rest;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.openl.rules.ruleservice.deployer.RulesDeployerRestController;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSExceptionMapper;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Factory bean for initializing of endpoint for {@link RulesDeployerRestController}
 *
 * @author Vladyslav Pikus
 */
public abstract class RulesDeployerRestControllerInitializingBean implements InitializingBean {

    private boolean isEnabled;
    private String baseAddress;

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public abstract RulesDeployerRestController getRulesDeployerRestController();

    public String getBaseAddress() {
        return baseAddress;
    }

    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
    }

    @Override
    public void afterPropertiesSet() {
        if (isEnabled) {
            JAXRSServerFactoryBean serverFactory = getJAXRSServerFactory();
            serverFactory.setServiceBean(getRulesDeployerRestController());
            serverFactory.setAddress(getBaseAddress());
            serverFactory.init();
        }
    }

    private JAXRSServerFactoryBean getJAXRSServerFactory() {
        JAXRSServerFactoryBean factoryBean = new JAXRSServerFactoryBean();
        factoryBean.setProvider(new JacksonJsonProvider());
        factoryBean.setProvider(new JAXRSExceptionMapper());
        factoryBean.setProvider(new WebApplicationExceptionMapper());
        return factoryBean;
    }
}
