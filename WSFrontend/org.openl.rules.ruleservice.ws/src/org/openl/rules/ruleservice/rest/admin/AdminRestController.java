package org.openl.rules.ruleservice.rest.admin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.publish.RuleServiceManager;
import org.openl.rules.ruleservice.servlet.ServiceInfo;

@Produces(MediaType.APPLICATION_JSON)
public class AdminRestController {

    private final RuleServiceManager ruleServiceManager;

    public AdminRestController(RuleServiceManager ruleServiceManager) {
        this.ruleServiceManager = ruleServiceManager;
    }

    /**
     * @return a list of descriptions of published OpenL services.
     */
    @GET
    @Path("/services")
    public Response getServiceInfo() {
        Collection<ServiceInfo> servicesInfo = PublisherUtils.getServicesInfo(ruleServiceManager);
        return Response.ok(new GenericEntity<Collection<ServiceInfo>>(servicesInfo) {
        }).build();
    }

    /**
     * @return a list of method descriptors of the given OpenL service.
     */
    @GET
    @Path("/services/{serviceName}/methods/")
    public Response getServiceMethodNames(
            @PathParam("serviceName") final String serviceName) throws RuleServiceInstantiationException {
        OpenLService service = ruleServiceManager.getServiceByName(serviceName);
        if (service == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        List<MethodDescriptor> methodsInfos = Arrays.stream(service.getServiceClass().getMethods())
            .map(this::toDescriptor)
            .sorted(Comparator.comparing(MethodDescriptor::getName, String::compareToIgnoreCase))
            .collect(Collectors.toList());
        return Response.ok(methodsInfos).build();
    }

    private MethodDescriptor toDescriptor(Method method) {
        String name = method.getName();
        String returnType = method.getReturnType().getSimpleName();
        List<String> paramTypes = Arrays.stream(method.getParameterTypes())
            .map(Class::getSimpleName)
            .collect(Collectors.toList());
        return new MethodDescriptor(name, returnType, paramTypes);
    }
}
