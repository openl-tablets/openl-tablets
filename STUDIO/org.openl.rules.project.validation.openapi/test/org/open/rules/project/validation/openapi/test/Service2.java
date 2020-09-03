package org.open.rules.project.validation.openapi.test;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.core.annotations.Name;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;

public interface Service2 {
    @POST
    @Path("/CapitalAdequacyScore1")
    Double CapitalAdequacyScore(IRulesRuntimeContext runtimeContext, Double score);

    @POST
    @Path("/BankRatingGroup1")
    String BankRatingGroup(IRulesRuntimeContext runtimeContext, Double arg0);

    @POST
    @Path("/BankRatingGroup")
    @ServiceExtraMethod(ServiceExtraMethodHandler2.class)
    String BankRatingGroupExtra(@Name("runtimeContext") IRulesRuntimeContext runtimeContext, @Name("bankRating") Double arg0);
}
