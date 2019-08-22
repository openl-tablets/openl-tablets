package org.openl.rules.ruleservice.rest.admin;

import java.util.Collection;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.openl.rules.ruleservice.servlet.PublisherUtils;
import org.openl.rules.ruleservice.servlet.ServiceInfo;
import org.openl.rules.ruleservice.servlet.ServiceMethodsInfo;

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

    /**
     * @return a list of method names.
     */
    @GET
    @Path("/services/{serviceName}/methods/")
    public Response getServiceMethodNames(
            @PathParam("serviceName") final String serviceName) throws RuleServiceInstantiationException {
        List<ServiceMethodsInfo> serviceMethods = PublisherUtils.getServiceMethods(ruleServicePublisher, serviceName);
        return Response.ok(new GenericEntity<Collection<ServiceMethodsInfo>>(serviceMethods) {
        }).build();
    }
}
