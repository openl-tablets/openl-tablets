package org.openl.rules.ruleservice.publish.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;

public class JAXRSInterfaceEnhancerHelperTest {

    public static interface TestInterface {
        void someMethod(String arg);
    }

    @Path("/test")
    public static interface TestAnnotatedInterface {
        @Path("/someMethod/{arg}")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        String someMethod(@PathParam("arg") String arg);
    }

    @Test
    public void testNoAnnotationMethod() throws Exception {
        Class<?> enchancedClass = JAXRSInterfaceEnhancerHelper.decorateInterface(TestInterface.class);
        boolean f = false;
        boolean producesAnnotationExists = false;
        boolean consumesAnnotationExists = false;

        for (Annotation annotation : enchancedClass.getAnnotations()) {
            if (annotation.annotationType().equals(Path.class)) {
                Path path = (Path) annotation;
                String value = path.value();
                if (value.equals("/")) {
                    f = true;
                }
            }
            if (annotation.annotationType().equals(Produces.class)) {
                producesAnnotationExists = true;
                Produces produces = (Produces) annotation;
                Assert.assertTrue("@Produces annotatoion requires values!", produces.value().length > 0);
            }
            if (annotation.annotationType().equals(Consumes.class)) {
                consumesAnnotationExists = true;
                Consumes consumes = (Consumes) annotation;
                Assert.assertTrue("@Consumes annotatoion requires values!", consumes.value().length > 0);
            }
        }

        if (!producesAnnotationExists) {
            Assert.fail("@Produces annotation is required!");
        }
        if (!consumesAnnotationExists) {
            Assert.fail("@Consumes annotation is required!");
        }

        if (!f) {
            Assert.fail("Enchanced interface should contains @Path annotation on class!");
        }

        boolean pathAnnotationExists = false;
        boolean getAnnotationExists = false;
        boolean pathParamAnnotationExists = false;

        Method someMethod = enchancedClass.getMethod("someMethod", String.class);
        for (Annotation annotation : someMethod.getAnnotations()) {
            if (annotation.annotationType().equals(Path.class)) {
                Path path = (Path) annotation;
                String value = path.value();
                pathAnnotationExists = true;
                if (!value.startsWith("/someMethod")) {
                    Assert.fail("Generated method should contains @Path annotation on method with method name value!");
                }
            }
            if (annotation.annotationType().equals(GET.class)) {
                getAnnotationExists = true;
            }
        }
        for (Annotation[] annotations : someMethod.getParameterAnnotations()) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(PathParam.class)) {
                    pathParamAnnotationExists = true;
                    PathParam param = (PathParam) annotation;
                    Assert.assertNotNull("Method parameter requred @PathParam annotation", param.value());
                }
            }
        }
        if (!pathParamAnnotationExists) {
            Assert.fail("@PathParam annotation is required!");
        }
        if (!pathAnnotationExists) {
            Assert.fail("@Path annotation is required!");
        }
        if (!getAnnotationExists) {
            Assert.fail("@GET annotation is required!");
        }
    }

    @Test
    public void testMethodWithAnnotation() throws Exception {
        Class<?> enchancedClass = JAXRSInterfaceEnhancerHelper.decorateInterface(TestAnnotatedInterface.class);
        boolean f = false;
        for (Annotation annotation : enchancedClass.getAnnotations()) {
            if (annotation.annotationType().equals(Path.class)) {
                Path path = (Path) annotation;
                String value = path.value();
                if (value.equals("/test")) {
                    f = true;
                }
            }
        }

        if (!f) {
            Assert.fail("Enchanced interface should contains @Path annotation on class!");
        }

        boolean pathAnnotationExists = false;
        boolean getAnnotationExists = false;
        boolean producesAnnotationExists = false;
        boolean consumesAnnotationExists = false;
        boolean pathParamAnnotationExists = false;

        Method someMethod = enchancedClass.getMethod("someMethod", String.class);
        for (Annotation annotation : someMethod.getAnnotations()) {
            if (annotation.annotationType().equals(Path.class)) {
                Path path = (Path) annotation;
                String value = path.value();
                pathAnnotationExists = true;
                if (!value.equals("/someMethod/{arg}")) {
                    Assert.fail("Generated method should contains @Path annotation on method with defined value!");
                }
            }
            if (annotation.annotationType().equals(GET.class)) {
                getAnnotationExists = true;
            }
            if (annotation.annotationType().equals(Produces.class)) {
                producesAnnotationExists = true;
                Produces produces = (Produces) annotation;
                Assert.assertTrue("@Produces annotatoion requires defined values!", produces.value().length == 1);
            }
            if (annotation.annotationType().equals(Consumes.class)) {
                consumesAnnotationExists = true;
                Consumes consumes = (Consumes) annotation;
                Assert.assertTrue("@Consumes annotatoion requires defined values!", consumes.value().length == 1);
            }
        }
        for (Annotation[] annotations : someMethod.getParameterAnnotations()) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(PathParam.class)) {
                    pathParamAnnotationExists = true;
                    PathParam param = (PathParam) annotation;
                    Assert.assertEquals("Method parameter requred @PathParam annotation", "arg", param.value());
                }
            }
        }
        if (!pathParamAnnotationExists) {
            Assert.fail("@PathParam annotation is required!");
        }
        if (!pathAnnotationExists) {
            Assert.fail("@Path annotation is required!");
        }
        if (!getAnnotationExists) {
            Assert.fail("@GET annotation is required!");
        }
        if (!producesAnnotationExists) {
            Assert.fail("@Produces annotation is required!");
        }
        if (!consumesAnnotationExists) {
            Assert.fail("@Consumes annotation is required!");
        }
    }
}
