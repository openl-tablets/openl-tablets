import org.openl.rules.ruleservice.core.annotations.Name
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod
import org.openl.rules.ruleservice.core.interceptors.RulesType
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor

import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

abstract class MyService {

    abstract Integer parse(String num);

    @ServiceCallBeforeInterceptor([InputInterceptor.class])
    @ServiceCallAfterInterceptor([OutputInterceptor.class])
    abstract MyType parse1(@RulesType("java.lang.String") Object p);
    // will be skipped because of such method does not exist in rules

    @ServiceCallAfterInterceptor([ResponseInterceptor.class])
    abstract Response parse2(@RulesType("java.lang.String") Object p);

    @GET
    @Path("parse/{num}")
    abstract Integer parse3(@PathParam("num") String num);

    @POST
    @Path("parseX")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    @ServiceCallBeforeInterceptor([InputInterceptor.class])
    @ServiceCallAfterInterceptor([OutputInterceptor.class])
    abstract MyType parse4(String num);

    @GET
    @Path("parseXQueryParam")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    @ServiceCallBeforeInterceptor([InputInterceptor.class])
    @ServiceCallAfterInterceptor([OutputInterceptor.class])
    abstract MyType parse4QueryParam(@QueryParam("numParam") String num);

    @GET
    @Path("parseXPathParam/{num}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    @ServiceCallBeforeInterceptor([InputInterceptor.class])
    @ServiceCallAfterInterceptor([OutputInterceptor.class])
    abstract MyType parse4PathParam(@PathParam("num") String num);

    @ServiceExtraMethod(VirtualMethodHandler.class)
    abstract Double virtual(@Name("text") String num);

    @ServiceExtraMethod(VirtualMethodHandler.class)
    abstract Double virtual2(@Name("first") String num, @Name("second") String arg);

    abstract MyType ping(MyType type);

    @ServiceCallAfterInterceptor(value = [ToDoubleServiceMethodAfterAdvice.class,
            NoConvertorServiceMethodAfterAdvice.class])
    abstract Double parse5(String num);

    @ServiceCallAfterInterceptor(value = [OpenLTypeServiceMethodAfterAdvice.class])
    abstract Double parse6(String num);
}

