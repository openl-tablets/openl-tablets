package org.openl.rules.ruleservice.rest.admin;

import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.openl.rules.ruleservice.servlet.PublisherUtils;
import org.openl.rules.ruleservice.servlet.ServiceInfo;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

@Produces(MediaType.APPLICATION_JSON)
public class AdminRestController {

    private final RuleServicePublisher ruleServicePublisher;

    public AdminRestController(RuleServicePublisher ruleServicePublisher) {
        this.ruleServicePublisher = ruleServicePublisher;
    }

    /**
     * @return a list of project descriptions.
     */
    @GET
    @Path("/services")
    public Response getServiceInfo() {
        Collection<ServiceInfo> servicesInfo = PublisherUtils.getServicesInfo(ruleServicePublisher);
        return Response.ok(new GenericEntity<Collection<ServiceInfo>>(servicesInfo) {
        }).build();
    }
}
