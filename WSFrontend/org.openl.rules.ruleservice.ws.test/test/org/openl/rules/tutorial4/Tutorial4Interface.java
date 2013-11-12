package org.openl.rules.tutorial4;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.openl.generated.beans.Driver;
import org.openl.rules.ruleservice.core.interceptors.converters.StringArray;


public interface Tutorial4Interface {
    @Path("/coverage")
    @Produces("application/json")
    @GET
    StringArray getCoverage();

    @Path("/theft_rating")
    @Produces("application/json")
    @GET
    StringArray getTheft_rating();

    @Path("/driverAgeType")
    @Produces("application/json")
    @Consumes("application/json")
    @GET
    DriverAgeType driverAgeType(@QueryParam("driver") Driver driver);
}
