package org.openl.rules.project.validation.openapi.test;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;

public interface EPBDS10971Service {
    @POST
    @Path("/ExtractRatingDetails")
    @ServiceExtraMethod(ServiceExtraMethodHandlerImpl.class)
    SpreadsheetResult ExtractRatingDetails();
}
