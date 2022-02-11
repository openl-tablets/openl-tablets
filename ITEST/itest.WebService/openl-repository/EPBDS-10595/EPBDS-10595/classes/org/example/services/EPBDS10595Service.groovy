package org.example.services;

import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.interceptors.RulesType;

public interface EPBDS10595Service {
    Double myRule(@RulesType("MyDatatype") Object inputParam);

    @RulesType("MyDatatype")
    @ServiceExtraMethod(EPBDS10595ServiceExtraMethodHandler.class)
    Object returnMyDatatype(@RulesType("MyDatatype") Object inputParam);

    @ServiceExtraMethod(EPBDS10595ServiceExtraMethodHandler.class)
    Object inputMyDatatype(@RulesType("MyDatatype") Object inputParam);


    @RulesType("MyDatatype")
    Object[] myRule(@RulesType(value = "MyDatatype") Object inputParam, String x);

    Double myRule2(@RulesType(value = "MyDatatype") Object[] inputParam, String x);
}
