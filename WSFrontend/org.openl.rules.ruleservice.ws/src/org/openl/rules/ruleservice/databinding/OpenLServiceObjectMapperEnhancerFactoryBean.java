package org.openl.rules.ruleservice.databinding;

import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.OpenLServiceHolder;
import org.openl.rules.ruleservice.databinding.jackson.NonNullMixIn;
import org.openl.types.IOpenClass;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;

public class OpenLServiceObjectMapperEnhancerFactoryBean extends AbstractFactoryBean<ObjectMapper> {

    private JacksonObjectMapperFactoryBean jacksonObjectMapperFactoryBean;

    public JacksonObjectMapperFactoryBean getJacksonObjectMapperFactoryBean() {
        return jacksonObjectMapperFactoryBean;
    }

    public void setJacksonObjectMapperFactoryBean(JacksonObjectMapperFactoryBean jacksonObjectMapperFactoryBean) {
        this.jacksonObjectMapperFactoryBean = jacksonObjectMapperFactoryBean;
    }

    @Override
    public Class<?> getObjectType() {
        return ObjectMapper.class;
    }

    @Override
    protected ObjectMapper createInstance() throws Exception {
        ObjectMapper objectMapper = getJacksonObjectMapperFactoryBean().createJacksonObjectMapper();
        OpenLService openLService = OpenLServiceHolder.getInstance().get();
        if (openLService == null) {
            throw new ServiceConfigurationException("Failed to locate a service.");
        }
        if (openLService.getOpenClass() != null) {
            for (IOpenClass openClass : openLService.getOpenClass().getTypes()) {
                if (openClass instanceof CustomSpreadsheetResultOpenClass) {
                    if (objectMapper
                        .findMixInClassFor(((CustomSpreadsheetResultOpenClass) openClass).getBeanClass()) == null) {
                        objectMapper.addMixIn(((CustomSpreadsheetResultOpenClass) openClass).getBeanClass(),
                            NonNullMixIn.class);
                    }
                }
            }
            if (openLService.getOpenClass() instanceof XlsModuleOpenClass) {
                objectMapper.addMixIn(((XlsModuleOpenClass) openLService.getOpenClass())
                    .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                    .toCustomSpreadsheetResultOpenClass()
                    .getBeanClass(), NonNullMixIn.class);
            }
        }
        return objectMapper;
    }
}
