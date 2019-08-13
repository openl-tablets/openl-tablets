package org.openl.rules.ruleservice.databinding;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.OpenLServiceHolder;
import org.openl.types.IOpenMethod;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class OpenLServiceRootClassNamesBindingFactoryBean extends AbstractFactoryBean<Set<String>> {

    @Override
    protected Set<String> createInstance() throws Exception {
        OpenLService openLService = OpenLServiceHolder.getInstance().get();
        Objects.requireNonNull(openLService, "Failed to locate a service.");
        Set<String> ret = new HashSet<>();
        for (IOpenMethod method : openLService.getOpenClass().getMethods()) {
            if (method.getType() instanceof CustomSpreadsheetResultOpenClass) {
                CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) method
                    .getType();
                ret.add(customSpreadsheetResultOpenClass.getBeanClass().getName());
            }
        }
        return ret;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public Class<?> getObjectType() {
        return Set.class;
    }

}
