package org.openl.rules.ruleservice.databinding;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.OpenLServiceHolder;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public final class ServiceXlsModuleOpenClassObjectFactoryBean extends AbstractFactoryBean<XlsModuleOpenClass> {

    @Override
    public Class<?> getObjectType() {
        return XlsModuleOpenClass.class;
    }

    @Override
    protected XlsModuleOpenClass createInstance() throws Exception {
        OpenLService openLService = OpenLServiceHolder.getInstance().get();
        if (openLService == null) {
            throw new ServiceConfigurationException("Failed to locate a service.");
        }
        return (XlsModuleOpenClass) openLService.getOpenClass();
    }
}
