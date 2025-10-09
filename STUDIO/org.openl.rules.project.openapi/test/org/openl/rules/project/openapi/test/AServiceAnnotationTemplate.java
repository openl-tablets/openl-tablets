package org.openl.rules.project.openapi.test;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.openl.rules.ruleservice.core.interceptors.RulesType;

public abstract class AServiceAnnotationTemplate {

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public abstract Integer add(@FormParam("a") Integer a, @FormParam("b") Integer b);

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public abstract Integer addWrong(@FormParam("a") Integer a, @FormParam("b") Integer b);

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public abstract Integer addWrongHere(@FormParam("a") String a, @FormParam("b1") Integer b);

    @Path("/add1/{x1}/{y1}")
    public abstract Integer add1(@PathParam("x1") Integer a, @PathParam("y1") Integer b);

    @Path("/addWrong1/{x1}/{y1}")
    public abstract Integer addWrong1(@PathParam("x1") Integer a, @PathParam("y1") Integer b);

    @Path("/addWrongHere1/{x1}/{y1}")
    public abstract Integer addWrongHere1(@PathParam("x1") String a, @PathParam("y1") String b);

    @Path("/add2")
    public abstract Integer add2(@QueryParam("x") Integer a, @QueryParam("y") Integer b);

    @Path("/addWrong2")
    public abstract Integer addWrong2(@QueryParam("x") Integer a, @QueryParam("y") Integer b);

    @Path("/addWrongHere2")
    public abstract Integer addWrongHere2(@QueryParam("x") String a, @QueryParam("b") Integer b);

    @Path("/method1")
    public abstract Integer method1(@QueryParam("x") Integer a, @RulesType("MyDatatype") Object myDatatype);

    @Path("/method2")
    public abstract Integer method2(@QueryParam("x") Integer a, @RulesType("MyDatatype") Object myDatatype);

    @Path("/method3")
    public abstract Integer method3(@QueryParam("x") Integer a, @RulesType("MyDatatype") Object myDatatype);

    @Path("/m1")
    public abstract Integer m1(@RulesType("MyDatatype") Object myDatatype);

    @Path("/m2")
    public abstract Integer m2(@RulesType("MyDatatype") Object myDatatype);

    @Path("/m3")
    public abstract Integer m3(@RulesType("MyDatatype") Object myDatatype);

}
