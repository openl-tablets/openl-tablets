package org.openl.itest.service

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.openl.itest.service.internal.InputInterceptor;
import org.openl.itest.service.internal.MyType;
import org.openl.itest.service.internal.ExtraType;
import org.openl.itest.service.internal.OutputInterceptor;
import org.openl.itest.service.internal.PrepareInterceptor;
import org.openl.itest.service.internal.ResponseInterceptor;
import org.openl.itest.service.internal.VirtualMethodHandler;
import org.openl.rules.ruleservice.core.annotations.Name;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.interceptors.RulesType;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor;

interface MyService {

    Integer parse(String num);

    @ServiceCallBeforeInterceptor([InputInterceptor.class])
    @ServiceCallAfterInterceptor([OutputInterceptor.class])
    MyType parse1(@RulesType("java.lang.String") Object p);
    // will be skipped because of such method does not exist in rules

    @ServiceCallAfterInterceptor([ResponseInterceptor.class])
    Object parse2(@RulesType("java.lang.String") Object p);

    @GET
    @Path("parse/{num}")
    Integer parse3(@PathParam("num") String num);

    @POST
    @Path("parseX")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    @ServiceCallBeforeInterceptor([InputInterceptor.class])
    @ServiceCallAfterInterceptor([OutputInterceptor.class])
    MyType parse4(String num);

    @GET
    @Path("parseXQueryParam")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    @ServiceCallBeforeInterceptor([InputInterceptor.class])
    @ServiceCallAfterInterceptor([OutputInterceptor.class])
    MyType parse4QueryParam(@QueryParam("numParam") String num);

    @GET
    @Path("parseXPathParam/{num}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    @ServiceCallBeforeInterceptor([InputInterceptor.class])
    @ServiceCallAfterInterceptor([OutputInterceptor.class])
    MyType parse4PathParam(@PathParam("num") String num);

    @ServiceExtraMethod(VirtualMethodHandler.class)
    Double virtual(@Name("text") String num);

    @ServiceExtraMethod(VirtualMethodHandler.class)
    Double virtual2(@Name("first") String num, @Name("second") String arg);

    MyType ping(MyType type);

    @ServiceCallAfterInterceptor(value = [ToDoubleServiceMethodAfterAdvice.class,
            NoConvertorServiceMethodAfterAdvice.class])
    Double parse5(String num);

    @ServiceCallAfterInterceptor(value = [OpenLTypeServiceMethodAfterAdvice.class])
    Double parse6(String num);

    @ServiceCallBeforeInterceptor([PrepareInterceptor.class])
    String toStr(@RulesType("RuleType") ExtraType type);
}

