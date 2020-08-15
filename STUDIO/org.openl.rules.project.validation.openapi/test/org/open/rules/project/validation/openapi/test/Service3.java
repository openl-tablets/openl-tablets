package org.open.rules.project.validation.openapi.test;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.core.interceptors.AnyType;

@Path(value = "prefix")
public interface Service3 {
    @POST
    @Path("/BankRatingCalculation")
    Object BankRatingCalculation(IRulesRuntimeContext runtimeContext, @AnyType Object object);
}
