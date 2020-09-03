package org.open.rules.project.validation.openapi.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.interceptors.AnyType;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;

public interface Service1 {
    @ServiceCallAfterInterceptor
    SpreadsheetResult BankRatingCalculation(IRulesRuntimeContext runtimeContext, @AnyType Object bank);

    @GET
    Double ProfitDynamicScore(IRulesRuntimeContext runtimeContext, Double profitDynamic, Double profit);

    @GET
    @Path(value = "/BalanceDynamicIndexCalculation")
    Double BalanceDynamicIndexCalculation(IRulesRuntimeContext context,
            @AnyType Object financialData,
            @AnyType Object financialData1);

    @ServiceExtraMethod(value = ServiceExtraMethodHandler1.class)
    Double extraMethod(Double score);
}
