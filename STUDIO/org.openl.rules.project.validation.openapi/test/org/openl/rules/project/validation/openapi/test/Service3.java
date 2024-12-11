package org.openl.rules.project.validation.openapi.test;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.core.interceptors.RulesType;

@Path(value = "prefix")
public interface Service3 {
    @POST
    @Path("/BankRatingCalculation")
    Object BankRatingCalculation(IRulesRuntimeContext runtimeContext, @RulesType("Bank") Object object);
}
