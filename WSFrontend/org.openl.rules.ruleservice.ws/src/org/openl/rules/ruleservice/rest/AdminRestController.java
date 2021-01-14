package org.openl.rules.ruleservice.rest;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.openl.info.OpenLVersion;
import org.openl.info.SysInfo;
import org.openl.rules.ruleservice.publish.JAXRSRuleServicePublisher;
import org.openl.rules.ruleservice.servlet.ServiceInfoProvider;
import org.springframework.beans.factory.annotation.Autowired;

@Produces(MediaType.APPLICATION_JSON)
public class AdminRestController {

    private ServiceInfoProvider serviceManager;
    private JAXRSRuleServicePublisher jaxrsRuleServicePublisher;
    private Map<String, Object> uiConfig;

    @Resource
    public void setServiceManager(ServiceInfoProvider serviceManager) {
        this.serviceManager = serviceManager;
    }

    @Resource
    public void setUiConfig(Map<String, Object> uiConfig) {
        this.uiConfig = uiConfig;
    }

    @Autowired
    public void setJaxrsRuleServicePublisher(JAXRSRuleServicePublisher jaxrsRuleServicePublisher) {
        this.jaxrsRuleServicePublisher = jaxrsRuleServicePublisher;
    }

    /**
     * @return a list of descriptions of published OpenL services.
     */
    @GET
    @Path("/services")
    public Response getServiceInfo() {
        return Response.ok(serviceManager.getServicesInfo()).build();
    }

    /**
     * @return a list of descriptions of published OpenL services with serverSettings.
     */
    @GET
    @Path("/ui/info")
    public Response getServiceInfoWithSettings() {
        Map<String, Object> info = new HashMap<>(uiConfig);
        info.put("services", serviceManager.getServicesInfo());
        info.put("noWadlServices", jaxrsRuleServicePublisher.listNoWadlServices());
        return Response.ok(info).build();
    }

    /**
     * @return a list of JVM metrics for monitoring purposes.
     */
    @GET
    @Path("/info/sys.json")
    public Response getSysInfo() {
        return Response.ok(SysInfo.get()).build();
    }


    /**
     * @return a list of properties about the OpenL build.
     */
    @GET
    @Path("/info/openl.json")
    public Response getOpenLInfo() {
        return Response.ok(OpenLVersion.get()).build();
    }

    /**
     * @return a list of method descriptors of the given OpenL service.
     */
    @GET
    @Path("/services/{serviceName:.+}/methods/")
    public Response getServiceMethodNames(@PathParam("serviceName") final String serviceName) {
        return okOrNotFound(serviceManager.getServiceMethods(serviceName));
    }

    /**
     * @return a list of messages of the given OpenL service.
     */
    @GET
    //space
    @Path("/services/{serviceName:.+}/errors/")
    public Response getServiceErrors(@PathParam("serviceName") final String serviceName) {
        return okOrNotFound(serviceManager.getServiceErrors(serviceName));
    }

    @GET
    @Path("/services/{serviceName:.+}/MANIFEST.MF")
    public Response getManifest(@PathParam("serviceName") final String serviceName) {
        return okOrNotFound(serviceManager.getManifest(serviceName));
    }

    private static Response okOrNotFound(Object entity) {
        return Response.status(entity == null ? Response.Status.NOT_FOUND : Response.Status.OK).entity(entity).build();
    }
}
