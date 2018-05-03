package org.openl.rules.ruleservice.publish.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;

public class JAXRSInterfaceEnhancerHelperTest {

    public static interface TestInterface {
        void someMethod(int arg);
    }

    public static interface TestParameterInterface {
        void someMethod3();

        void someMethod4(int arg0, int arg2, int arg3, int arg4, int arg5);

        void someMethod2(int i, String arg0);

        void someMethod(String arg0, String arg1);
    }

    public static interface TestMethodNameAndPath {
        @Path("/someMethod1")
        void someMethod();

        void someMethod(int arg0, int arg2, int arg3);

        void someMethod(int arg0, String arg1);
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
        Class<?> enchancedClass = JAXRSInterfaceEnhancerHelper.decorateInterface(TestInterface.class, null);
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

        Method someMethod = enchancedClass.getMethod("someMethod", int.class);
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
        Class<?> enchancedClass = JAXRSInterfaceEnhancerHelper.decorateInterface(TestAnnotatedInterface.class, null);
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
                Assert.assertEquals("@Produces annotatoion requires defined values!", 1, produces.value().length);
            }
            if (annotation.annotationType().equals(Consumes.class)) {
                consumesAnnotationExists = true;
                Consumes consumes = (Consumes) annotation;
                Assert.assertEquals("@Consumes annotatoion requires defined values!", 1, consumes.value().length);
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

    @Test
    public void testParametersInMethod() throws Exception {
        Class<?> enchancedClass = JAXRSInterfaceEnhancerHelper.decorateInterface(TestParameterInterface.class, null);
        int i = 0;
        for (Method method : enchancedClass.getMethods()) {
            if ("someMethod".equals(method.getName())) {
                i++;
                Annotation postAnnotation = method.getAnnotation(POST.class);
                Assert.assertNotNull("Expected POST annotation!", postAnnotation);

                Assert.assertEquals("Expected only one parameter in method!", 1, method.getParameterTypes().length);
            }
            if ("someMethod4".equals(method.getName())) {
                i++;
                Annotation postAnnotation = method.getAnnotation(POST.class);
                Assert.assertNotNull("Expected POST annotation!", postAnnotation);

                Assert.assertEquals("Expected only one parameter in method!", 1, method.getParameterTypes().length);
            }
            if ("someMethod2".equals(method.getName())) {
                i++;
                Annotation getAnnotation = method.getAnnotation(POST.class);
                Assert.assertNotNull("Expected POST annotation!", getAnnotation);

                Assert.assertEquals("Expected one parameters in method!", 1, method.getParameterTypes().length);
            }
            if ("someMethod3".equals(method.getName())) {
                i++;
                Annotation getAnnotation = method.getAnnotation(GET.class);
                Assert.assertNotNull("Expected GET annotation!", getAnnotation);

                Assert.assertEquals("Expected no parameters in method!", 0, method.getParameterTypes().length);
            }
        }

        Assert.assertEquals("Method is not found!", 4, i);
    }

    @Test
    public void testMethodNamesAndPath() throws Exception {
        Class<?> enchancedClass = JAXRSInterfaceEnhancerHelper.decorateInterface(TestMethodNameAndPath.class, null);
        Method[] methods = enchancedClass.getMethods();
        Assert.assertEquals("Method is not found!", 3, methods.length);

        for (Method method : methods) {
            Assert.assertEquals("someMethod", method.getName());
            Annotation pathAnnotation = method.getAnnotation(Path.class);
            Assert.assertNotNull("Expected @Path annotation!", pathAnnotation);

            String path = null;
            switch (method.getParameterTypes().length) {
                case 0:
                    path = "/someMethod1";
                    break;
                case 1:
                    path = "/someMethod";
                    break;
                case 3:
                    path = "/someMethod2/{arg0: .*}/{arg1: .*}/{arg2: .*}";
                    break;
                default:
                    Assert.fail("Unexpected count of arguments");


            }
            Assert.assertEquals("Unexpected value in @Path annotation", path, ((Path)pathAnnotation).value());
        }
    }
}
