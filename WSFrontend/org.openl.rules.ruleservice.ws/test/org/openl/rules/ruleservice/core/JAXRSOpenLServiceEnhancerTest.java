package org.openl.rules.ruleservice.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.ruleservice.jaxrs.JAXRSOpenLServiceEnhancer;

public class JAXRSOpenLServiceEnhancerTest {

    public interface TestInterface {
        void someMethod(int arg);
    }

    public interface TestParameterInterface {
        void someMethod3();

        void someMethod4(int arg0, int arg2, int arg3, int arg4, int arg5);

        void someMethod2(int i, String arg0);

        void someMethod(String arg0, String arg1);
    }

    public interface TestMethodNameAndPath {
        @Path("/someMethod1")
        void someMethod();

        void someMethod(int arg0, int arg2, int arg3);

        void someMethod(int arg0, String arg1);
    }

    @Path("/test")
    public interface TestAnnotatedInterface {
        @Path("/someMethod/{arg}")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        String someMethod(@PathParam("arg") String arg);
    }

    @Test
    public void testNoAnnotationMethod() throws Exception {
        Class<?> enhancedClass = createService(TestInterface.class);
        boolean f = false;
        boolean producesAnnotationExists = false;
        boolean consumesAnnotationExists = false;

        for (Annotation annotation : enhancedClass.getAnnotations()) {
            if (annotation instanceof Path) {
                Path path = (Path) annotation;
                String value = path.value();
                if (value.equals("/")) {
                    f = true;
                }
            }
            if (annotation instanceof Produces) {
                producesAnnotationExists = true;
                Produces produces = (Produces) annotation;
                assertTrue(produces.value().length > 0, "@Produces annotation requires values.");
            }
            if (annotation instanceof Consumes) {
                consumesAnnotationExists = true;
                Consumes consumes = (Consumes) annotation;
                assertTrue(consumes.value().length > 0, "@Consumes annotation requires values.");
            }
        }

        if (!producesAnnotationExists) {
            fail("@Produces annotation is required.");
        }
        if (!consumesAnnotationExists) {
            fail("@Consumes annotation is required.");
        }

        if (!f) {
            fail("Enhanced interface should contains @Path annotation on class.");
        }

        boolean pathAnnotationExists = false;
        boolean getAnnotationExists = false;
        boolean pathParamAnnotationExists = false;

        Method someMethod = enhancedClass.getMethod("someMethod", int.class);
        for (Annotation annotation : someMethod.getAnnotations()) {
            if (annotation instanceof Path) {
                Path path = (Path) annotation;
                String value = path.value();
                pathAnnotationExists = true;
                if (!value.startsWith("/someMethod")) {
                    fail("Generated method should contains @Path annotation on method with method name value.");
                }
            }
            if (annotation instanceof GET) {
                getAnnotationExists = true;
            }
        }
        for (Parameter parameter : someMethod.getParameters()) {
            PathParam param = parameter.getAnnotation(PathParam.class);
            if (param != null) {
                pathParamAnnotationExists = true;
                assertNotNull(param.value(), "Method parameter required @PathParam annotation");
            }
        }
        if (!pathParamAnnotationExists) {
            fail("@PathParam annotation is required.");
        }
        if (!pathAnnotationExists) {
            fail("@Path annotation is required.");
        }
        if (!getAnnotationExists) {
            fail("@GET annotation is required.");
        }
    }

    public interface TestAnnotatedInterface1 {
        @Path("/someMethod/{arg1}")
        @GET
        String someMethod(@PathParam("arg1") String arg, String arg2);
    }

    @Test
    public void testMethodWithAnnotation1() throws Exception {
        Class<?> enhancedClass = createService(TestAnnotatedInterface1.class);
        Method someMethod = enhancedClass.getMethod("someMethod", String.class, String.class);
        Path path = someMethod.getAnnotation(Path.class);
        assertNotNull(path);
        assertEquals("/someMethod/{arg1}", path.value());
        assertNotNull(someMethod.getAnnotation(GET.class));
        assertNull(someMethod.getAnnotation(POST.class));
        Parameter parameter1 = someMethod.getParameters()[0];
        PathParam pathParam = parameter1.getAnnotation(PathParam.class);
        assertNotNull(pathParam, "Expected @PathParam annotation");
        assertEquals("arg1", pathParam.value());
        Parameter parameter2 = someMethod.getParameters()[1];
        QueryParam queryParam = parameter2.getAnnotation(QueryParam.class);
        assertNotNull(queryParam, "Expected @QueryParam annotation");
        assertEquals("arg1", queryParam.value());
    }

    public interface TestAnnotatedInterface2 {
        @POST
        String someMethod(String arg1, String arg2);
    }

    @Test
    public void testMethodWithAnnotation2() throws Exception {
        Class<?> enhancedClass = createService(TestAnnotatedInterface2.class);
        Method someMethod = null;
        for (Method method : enhancedClass.getMethods()) {
            if ("someMethod".equals(method.getName())) {
                someMethod = method;
                break;
            }
        }
        assertNotNull(someMethod);
        Path path = someMethod.getAnnotation(Path.class);
        assertNotNull(path);
        assertEquals("/someMethod", path.value());
        assertNotNull(someMethod.getAnnotation(POST.class));
        assertNull(someMethod.getAnnotation(GET.class));
        assertEquals(1, someMethod.getParameterCount());
    }

    public interface TestAnnotatedInterface3 {
        @POST
        @Path("/value")
        String someMethod(String arg1, String arg2);
    }

    @Test
    public void testMethodWithAnnotation3() throws Exception {
        Class<?> enhancedClass = createService(TestAnnotatedInterface3.class);
        Method someMethod = null;
        for (Method method : enhancedClass.getMethods()) {
            if ("someMethod".equals(method.getName())) {
                someMethod = method;
                break;
            }
        }
        assertNotNull(someMethod);
        Path path = someMethod.getAnnotation(Path.class);
        assertNotNull(path);
        assertEquals("/value", path.value());
        assertNotNull(someMethod.getAnnotation(POST.class));
        assertNull(someMethod.getAnnotation(GET.class));
        assertEquals(1, someMethod.getParameterCount());
    }

    @Test
    public void testMethodWithAnnotation() throws Exception {
        Class<?> enhancedClass = createService(TestAnnotatedInterface.class);
        boolean f = false;
        Annotation pathAnnotation = enhancedClass.getAnnotation(Path.class);
        if (pathAnnotation != null) {
            Path path = (Path) pathAnnotation;
            String value = path.value();
            if (value.equals("/test")) {
                f = true;
            }
        }

        if (!f) {
            fail("Enchanted interface should contains @Path annotation on class.");
        }

        boolean pathAnnotationExists = false;
        boolean getAnnotationExists = false;
        boolean producesAnnotationExists = false;
        boolean consumesAnnotationExists = false;
        boolean pathParamAnnotationExists = false;

        Method someMethod = enhancedClass.getMethod("someMethod", String.class);
        for (Annotation annotation : someMethod.getAnnotations()) {
            if (annotation instanceof Path) {
                Path path = (Path) annotation;
                String value = path.value();
                pathAnnotationExists = true;
                if (!value.equals("/someMethod/{arg}")) {
                    fail("Generated method should contains @Path annotation on method with defined value.");
                }
            }
            if (annotation instanceof GET) {
                getAnnotationExists = true;
            }
            if (annotation instanceof Produces) {
                producesAnnotationExists = true;
                Produces produces = (Produces) annotation;
                assertEquals(1, produces.value().length, "@Produces annotation requires defined values.");
            }
            if (annotation instanceof Consumes) {
                consumesAnnotationExists = true;
                Consumes consumes = (Consumes) annotation;
                assertEquals(1, consumes.value().length, "@Consumes annotation requires defined values.");
            }
        }
        for (Parameter parameter : someMethod.getParameters()) {
            PathParam param = parameter.getAnnotation(PathParam.class);
            if (param != null) {
                pathParamAnnotationExists = true;
                assertEquals("arg", param.value(), "Method parameter required @PathParam annotation");
            }
        }
        if (!pathParamAnnotationExists) {
            fail("@PathParam annotation is required.");
        }
        if (!pathAnnotationExists) {
            fail("@Path annotation is required.");
        }
        if (!getAnnotationExists) {
            fail("@GET annotation is required.");
        }
        if (!producesAnnotationExists) {
            fail("@Produces annotation is required.");
        }
        if (!consumesAnnotationExists) {
            fail("@Consumes annotation is required.");
        }
    }

    @Test
    public void testParametersInMethod() throws Exception {
        Class<?> enhancedClass = createService(TestParameterInterface.class);
        int i = 0;
        for (Method method : enhancedClass.getMethods()) {
            if ("someMethod".equals(method.getName())) {
                i++;
                Annotation postAnnotation = method.getAnnotation(POST.class);
                assertNotNull(postAnnotation, "Expected POST annotation.");

                assertEquals(1, method.getParameterTypes().length, "Expected only one parameter in method.");
            }
            if ("someMethod4".equals(method.getName())) {
                i++;
                Annotation postAnnotation = method.getAnnotation(POST.class);
                assertNotNull(postAnnotation, "Expected POST annotation.");

                assertEquals(1, method.getParameterTypes().length, "Expected only one parameter in method.");
            }
            if ("someMethod2".equals(method.getName())) {
                i++;
                Annotation getAnnotation = method.getAnnotation(POST.class);
                assertNotNull(getAnnotation, "Expected POST annotation.");

                assertEquals(1, method.getParameterTypes().length, "Expected one parameters in method.");
            }
            if ("someMethod3".equals(method.getName())) {
                i++;
                Annotation getAnnotation = method.getAnnotation(GET.class);
                assertNotNull(getAnnotation, "Expected GET annotation.");

                assertEquals(0, method.getParameterTypes().length, "Expected no parameters in method.");
            }
        }

        assertEquals(4, i, "Method is not found.");
    }

    @Test
    public void testMethodNamesAndPath() throws Exception {
        Class<?> enhancedClass = createService(TestMethodNameAndPath.class);
        Method[] methods = enhancedClass.getMethods();
        assertEquals(3, methods.length, "Method is not found.");

        for (Method method : methods) {
            assertEquals("someMethod", method.getName());
            Annotation pathAnnotation = method.getAnnotation(Path.class);
            assertNotNull(pathAnnotation, "Expected @Path annotation.");

            String path = null;
            switch (method.getParameterCount()) {
                case 0:
                    path = "/someMethod1";
                    break;
                case 1:
                    path = "/someMethod";
                    break;
                case 3:
                    path = "/someMethod/{arg0: .*}/{arg1: .*}/{arg2: .*}";
                    break;
                default:
                    fail("Unexpected count of arguments");

            }
            assertEquals(path, ((Path) pathAnnotation).value(), "Unexpected value in @Path annotation");
        }
    }

    public interface TestNotAnnotatedByApiResponsesInterface {
        @POST
        @Path("/value")
        String someMethod(String arg);
    }

    @Test
    public void shouldAddApiResponsesIfNotAnnotatedByApiResponses() throws Exception {
        Class<?> enhancedClass = createService(TestNotAnnotatedByApiResponsesInterface.class);
        Method someMethod = enhancedClass.getMethod("someMethod", String.class);
        boolean apiResponsesAnnotationExists = false;
        for (Annotation annotation : someMethod.getAnnotations()) {
            if (annotation instanceof ApiResponses) {
                apiResponsesAnnotationExists = true;
            }
        }
        if (!apiResponsesAnnotationExists) {
            fail("@ApiResponses annotation should be added");
        }
    }

    public interface TestWithOperationNotAnnotatedByApiResponsesInterface {
        @POST
        @Path("/value")
        @Operation(summary = "Download resource file", tags = "Resource")
        String someMethod(String arg);
    }

    @Test
    public void shouldAddApiResponsesIfOperationNotAnnotatedByApiResponses() throws Exception {
        Class<?> enhancedClass = createService(TestNotAnnotatedByApiResponsesInterface.class);
        Method someMethod = enhancedClass.getMethod("someMethod", String.class);
        boolean apiResponsesAnnotationExists = false;
        for (Annotation annotation : someMethod.getAnnotations()) {
            if (annotation instanceof ApiResponses) {
                apiResponsesAnnotationExists = true;
            }
        }
        if (!apiResponsesAnnotationExists) {
            fail("@ApiResponses annotation should be added");
        }
    }

    public interface TestAnnotatedByApiResponseInterface {
        @POST
        @Path("/value")
        @ApiResponse(responseCode = "200", description = "@ApiResponse Annotated")
        String someMethod(String arg);
    }

    @Test
    public void shouldNotAddApiResponsesIfAnnotatedByApiResponse() throws Exception {
        Class<?> enhancedClass = createService(TestAnnotatedByApiResponseInterface.class);
        Method someMethod = enhancedClass.getMethod("someMethod", String.class);
        boolean apiResponsesAnnotationExists = false;
        for (Annotation annotation : someMethod.getAnnotations()) {
            if (annotation instanceof ApiResponses) {
                apiResponsesAnnotationExists = true;
                break;
            }
        }
        if (apiResponsesAnnotationExists) {
            fail("@ApiResponses annotation shouldn't be added if @ApiResponse exists");
        }
    }

    public interface TestAnnotatedByOperationWithResponsesInterface {
        @POST
        @Path("/value")
        @Operation(summary = "Download resource file", tags = "Resource",
                responses = {@ApiResponse(responseCode = "200", description = "@OPERATION Annotated"),
                        @ApiResponse(responseCode = "204", description = "@OPERATION Annotated")})
        String someMethod(String arg);
    }

    @Test
    public void shouldNotAddApiResponsesIfAlreadyDefinedInOperationAnnotation() throws Exception {
        Class<?> enhancedClass = createService(TestAnnotatedByOperationWithResponsesInterface.class);
        Method someMethod = enhancedClass.getMethod("someMethod", String.class);
        boolean apiResponsesAnnotationExists = false;
        for (Annotation annotation : someMethod.getAnnotations()) {
            if (annotation instanceof ApiResponses) {
                apiResponsesAnnotationExists = true;
                break;
            }
        }
        if (apiResponsesAnnotationExists) {
            fail("@ApiResponses annotation shouldn't be added");
        }
    }

    private static Class<?> createService(Class<?> clazz) throws Exception {
        ClassLoader classLoader = new ClassLoader() {
        };
        OpenLService service = new OpenLService.OpenLServiceBuilder().setClassLoader(classLoader)
                .setName("test")
                .setDeployPath("testPath")
                .setDeployment(new DeploymentDescription("testPath", new CommonVersionImpl("0")))
                .setServiceClass(clazz)
                .build(new AbstractOpenLServiceInitializer() {
                    @Override
                    protected void init(OpenLService openLService) {
                    }
                });
        service.setServiceBean(new Object());
        Object proxy = new JAXRSOpenLServiceEnhancer().decorateServiceBean(service);
        return proxy.getClass().getInterfaces()[0];
    }
}
