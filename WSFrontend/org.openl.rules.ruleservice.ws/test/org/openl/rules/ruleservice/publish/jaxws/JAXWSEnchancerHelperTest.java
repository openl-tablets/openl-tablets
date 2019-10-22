package org.openl.rules.ruleservice.publish.jaxws;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.ws.rs.PathParam;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.ruleservice.core.AbstractOpenLServiceInitializer;
import org.openl.rules.ruleservice.core.OpenLService;

public class JAXWSEnchancerHelperTest {
    public interface TestInterface {
        void someMethod(String arg);
    }

    public interface TestParameterInterface {
        void someMethod3();

        void someMethod4(int arg0, int arg2, int arg3, int arg4, int arg5);

        void someMethod2(int i, String arg0);

        void someMethod(String arg0, String arg1);
    }

    public interface TestMethodNamesAndOperationNames {
        @WebMethod(operationName = "someMethod1")
        void someMethod();

        void someMethod(int arg0, int arg2, int arg3);

        void someMethod(int arg0, String arg1);
    }

    @WebService(name = "TestAnnotatedInterface2")
    public interface TestAnnotatedInterface {
        @WebMethod(operationName = "someMethod1")
        String someMethod(@PathParam("arg") String arg);
    }

    @Test
    public void testNoAnnotationMethod() throws Exception {
        ClassLoader classLoader = new ClassLoader() {
        };
        OpenLService service = new OpenLService.OpenLServiceBuilder().setClassLoader(classLoader)
            .setName("test")
            .setServiceClass(TestInterface.class)
            .build(new AbstractOpenLServiceInitializer() {
                @Override
                protected void init(OpenLService openLService) {
                }
            });

        Class<?> enchancedClass = JAXWSEnhancerHelper.decorateServiceInterface(service);

        Annotation webServiceAnnotation = enchancedClass.getAnnotation(WebService.class);
        Assert.assertNotNull("Enchanced interface should contains @WebService annotation on class.",
            webServiceAnnotation);

        Method someMethod = enchancedClass.getMethod("someMethod", String.class);

        Annotation webMethodAnnotatopn = someMethod.getAnnotation(WebMethod.class);
        Assert.assertNotNull("Generated method should contains @WebMethod annotation.", webMethodAnnotatopn);

        Assert.assertEquals("Generated method should contains @WebMethod annotation on method with operation name.",
            "someMethod",
            ((WebMethod) webMethodAnnotatopn).operationName());
    }

    @Test
    public void testMethodWithAnnotation() throws Exception {
        ClassLoader classLoader = new ClassLoader() {
        };
        OpenLService service = new OpenLService.OpenLServiceBuilder().setClassLoader(classLoader)
            .setName("test")
            .setServiceClass(TestAnnotatedInterface.class)
            .build(new AbstractOpenLServiceInitializer() {
                @Override
                protected void init(OpenLService openLService) {
                }
            });
        Class<?> enchancedClass = JAXWSEnhancerHelper.decorateServiceInterface(service);

        Annotation webServiceAnnotation = enchancedClass.getAnnotation(WebService.class);
        Assert.assertNotNull("Enchanced interface should contains @WebService annotation on class.",
            webServiceAnnotation);

        Assert.assertEquals("Generated method should contains @WebService annotation with defined in interface name.",
            "TestAnnotatedInterface2",
            ((WebService) webServiceAnnotation).name());

        Method someMethod = enchancedClass.getMethod("someMethod", String.class);

        Annotation webMethodAnnotatopn = someMethod.getAnnotation(WebMethod.class);
        Assert.assertNotNull("Generated method should contains @WebMethod annotation.", webMethodAnnotatopn);

        Assert.assertEquals(
            "Generated method should contains @WebMethod annotation on method with defined in interface operation name.",
            "someMethod1",
            ((WebMethod) webMethodAnnotatopn).operationName());
    }

    @Test
    public void testMethodNamesAndOperationNames() throws Exception {
        ClassLoader classLoader = new ClassLoader() {
        };
        OpenLService service = new OpenLService.OpenLServiceBuilder().setClassLoader(classLoader)
            .setName("test")
            .setServiceClass(TestMethodNamesAndOperationNames.class)
            .build(new AbstractOpenLServiceInitializer() {
                @Override
                protected void init(OpenLService openLService) {
                }
            });
        Class<?> enchancedClass = JAXWSEnhancerHelper.decorateServiceInterface(service);
        int i = 0;
        for (Method method : enchancedClass.getMethods()) {
            if ("someMethod".equals(method.getName()) && method.getParameterTypes().length == 0) {
                i++;
                Annotation webMethodAnnotation = method.getAnnotation(WebMethod.class);
                Assert.assertNotNull("Expected @WebMethod annotation.", webMethodAnnotation);
                Assert.assertEquals("Expected 'someMethod1' operation name in annotation.",
                    "someMethod1",
                    ((WebMethod) webMethodAnnotation).operationName());
            }
            if ("someMethod".equals(method.getName()) && method.getParameterTypes().length == 2) {
                i++;
                Annotation webMethodAnnotation = method.getAnnotation(WebMethod.class);
                Assert.assertNotNull("Expected @WebMethod annotation.", webMethodAnnotation);

                Assert.assertEquals("Expected 'someMethod' operation name in annotation.",
                    "someMethod",
                    ((WebMethod) webMethodAnnotation).operationName());
            }
            if ("someMethod".equals(method.getName()) && method.getParameterTypes().length == 3) {
                i++;
                Annotation webMethodAnnotation = method.getAnnotation(WebMethod.class);
                Assert.assertNotNull("Expected @WebMethod annotation.", webMethodAnnotation);

                Assert.assertEquals("Expected 'someMethod2' operation name in annotation.",
                    "someMethod2",
                    ((WebMethod) webMethodAnnotation).operationName());
            }
        }

        Assert.assertTrue("Method is not found.", i == 3);
    }
}
