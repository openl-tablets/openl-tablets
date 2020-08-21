package org.openl.rules.ruleservice.databinding;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.OpenLServiceHolder;
import org.openl.rules.serialization.JacksonObjectMapperFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class ServiceJacksonObjectMapperEnhancerFactoryBean extends AbstractFactoryBean<ObjectMapper> {

    private JacksonObjectMapperFactory jacksonObjectMapperFactory;

    public JacksonObjectMapperFactory getJacksonObjectMapperFactory() {
        return jacksonObjectMapperFactory;
    }

    public void setJacksonObjectMapperFactory(JacksonObjectMapperFactory jacksonObjectMapperFactory) {
        this.jacksonObjectMapperFactory = jacksonObjectMapperFactory;
    }

    @Override
    public Class<?> getObjectType() {
        return ObjectMapper.class;
    }

    @Override
    protected ObjectMapper createInstance() throws Exception {
        OpenLService openLService = OpenLServiceHolder.getInstance().get();
        if (openLService == null) {
            throw new ServiceConfigurationException("Failed to locate a service.");
        }

        ServiceJacksonObjectMapperEnhancer serviceJacksonObjectMapperEnhancer = new ServiceJacksonObjectMapperEnhancer(
            jacksonObjectMapperFactory.createJacksonObjectMapper(),
            (XlsModuleOpenClass) openLService.getOpenClass(),
            openLService.getClassLoader());

        return serviceJacksonObjectMapperEnhancer.createObjectMapper();
    }
}
