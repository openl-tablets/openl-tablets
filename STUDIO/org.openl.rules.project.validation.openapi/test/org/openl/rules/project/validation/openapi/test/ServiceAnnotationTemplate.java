package org.openl.rules.project.validation.openapi.test;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.openl.rules.ruleservice.core.interceptors.RulesType;

public interface ServiceAnnotationTemplate {
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Integer add(@FormParam("a") Integer a, @FormParam("b") Integer b);

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Integer addWrong(@FormParam("a") Integer a, @FormParam("b") Integer b);

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Integer addWrongHere(@FormParam("a") String a, @FormParam("b1") Integer b);

    @Path("/add1/{x1}/{y1}")
    Integer add1(@PathParam("x1") Integer a, @PathParam("y1") Integer b);

    @Path("/addWrong1/{x1}/{y1}")
    Integer addWrong1(@PathParam("x1") Integer a, @PathParam("y1") Integer b);

    @Path("/addWrongHere1/{x1}/{y1}")
    Integer addWrongHere1(@PathParam("x1") String a, @PathParam("y1") String b);

    @Path("/add2")
    Integer add2(@QueryParam("x") Integer a, @QueryParam("y") Integer b);

    @Path("/addWrong2")
    Integer addWrong2(@QueryParam("x") Integer a, @QueryParam("y") Integer b);

    @Path("/addWrongHere2")
    Integer addWrongHere2(@QueryParam("x") String a, @QueryParam("b") Integer b);

    @Path("/method1")
    Integer method1(@QueryParam("x") Integer a, @RulesType("MyDatatype") Object myDatatype);

    @Path("/method2")
    Integer method2(@QueryParam("x") Integer a, @RulesType("MyDatatype") Object myDatatype);

    @Path("/method3")
    Integer method3(@QueryParam("x") Integer a, @RulesType("MyDatatype") Object myDatatype);

    @Path("/m1")
    Integer m1(@RulesType("MyDatatype") Object myDatatype);

    @Path("/m2")
    Integer m2(@RulesType("MyDatatype") Object myDatatype);

    @Path("/m3")
    Integer m3(@RulesType("MyDatatype") Object myDatatype);
}
