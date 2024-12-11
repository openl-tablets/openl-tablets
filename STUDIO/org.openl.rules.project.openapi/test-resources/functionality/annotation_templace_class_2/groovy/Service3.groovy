package org.openl.rules.project.openapi.test

import org.openl.rules.context.IRulesRuntimeContext
import org.openl.rules.ruleservice.core.interceptors.RulesType

import jakarta.ws.rs.POST
import jakarta.ws.rs.Path

@Path(value = "prefix")
interface Service3 {
    @POST
    @Path("/BankRatingCalculation")
    Object BankRatingCalculation(IRulesRuntimeContext runtimeContext, @RulesType("Bank") Object object);
}

