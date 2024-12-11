package org.openl.rules.project.validation.openapi.test

import org.openl.rules.calc.SpreadsheetResult
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod

import jakarta.ws.rs.POST
import jakarta.ws.rs.Path

interface EPBDS10971Service {
    @POST
    @Path("/ExtractRatingDetails")
    @ServiceExtraMethod(ServiceExtraMethodHandlerImpl.class)
    SpreadsheetResult ExtractRatingDetails();
}

