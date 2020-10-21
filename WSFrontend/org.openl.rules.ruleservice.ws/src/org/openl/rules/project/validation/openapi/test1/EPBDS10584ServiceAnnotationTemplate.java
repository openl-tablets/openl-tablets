package org.openl.rules.project.validation.openapi.test1;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.openl.rules.ruleservice.core.interceptors.AnyType;

public interface EPBDS10584ServiceAnnotationTemplate {
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Integer add(@FormParam("a") Integer a, @FormParam("b") Integer b);

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Integer addWrong(@FormParam("a") Integer a, @FormParam("b") Integer b);

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Integer addWrongHere(@FormParam("a") Integer a, @FormParam("b") Integer b);

    @Path("/add1/{x1}/{y1}")
    Integer add1(@PathParam("x1") Integer a, @PathParam("y1") Integer b);

    @Path("/addWrong1/{x1}/{y1}")
    Integer addWrong1(@PathParam("x1") Integer a, @PathParam("y1") Integer b);

    @Path("/addWrongHere1/{x1}/{y1}")
    Integer addWrongHere1(@PathParam("x1") Integer a, @PathParam("y1") Integer b);

    @Path("/add2")
    Integer add2(@QueryParam("x") Integer a, @QueryParam("y") Integer b);

    @Path("/addWrong2")
    Integer addWrong2(@QueryParam("x") Integer a, @QueryParam("y") Integer b);

    @Path("/addWrongHere2")
    Integer addWrongHere2(@QueryParam("x") Integer a, @QueryParam("y") Integer b);

    @Path("/method1")
    Integer method1(@QueryParam("x") Integer a, @AnyType Object myDatatype);

    @Path("/method2")
    Integer method2(@QueryParam("x") Integer a, @AnyType Object myDatatype);

    @Path("/method3")
    Integer method3(@QueryParam("x") Integer a, @AnyType Object myDatatype);

    @Path("/m1")
    Object m1(@AnyType Object myDatatype);

    @Path("/m2")
    Object m2(@AnyType Object myDatatype);

    @Path("/m3")
    Object m3(@AnyType Object myDatatype);
}
