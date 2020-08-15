package org.open.rules.project.validation.openapi.test;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.openl.rules.context.IRulesRuntimeContext;

public interface Service2 {
    @POST
    @Path("/CapitalAdequacyScore1")
    Double CapitalAdequacyScore(IRulesRuntimeContext runtimeContext, Double score);
}
