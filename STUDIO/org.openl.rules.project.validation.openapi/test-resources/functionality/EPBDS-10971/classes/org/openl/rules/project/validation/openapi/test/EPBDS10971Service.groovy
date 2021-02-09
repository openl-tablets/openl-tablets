package org.openl.rules.project.validation.openapi.test

import org.openl.rules.calc.SpreadsheetResult
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod

import javax.ws.rs.POST
import javax.ws.rs.Path

interface EPBDS10971Service {
    @POST
    @Path("/ExtractRatingDetails")
    @ServiceExtraMethod(ServiceExtraMethodHandlerImpl.class)
    SpreadsheetResult ExtractRatingDetails();
}

