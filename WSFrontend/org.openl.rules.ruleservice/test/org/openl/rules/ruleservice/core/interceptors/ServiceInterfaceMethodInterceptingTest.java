package org.openl.rules.ruleservice.core.interceptors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.abstraction.IProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationFactoryHelper;
import org.openl.rules.ruleservice.core.RuleServiceOpenLServiceInstantiationFactoryImpl;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAroundInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;

@TestPropertySource(properties = {"some.prop = @Value"})
@SpringJUnitConfig(locations = {"classpath:openl-ruleservice-beans.xml"})
public class ServiceInterfaceMethodInterceptingTest {

    private static final String ELUSIVE_CLASS_NAME = "org.openl.test.MustNotBeFoundInClassloader";

    public static class ResultConverter extends AbstractServiceMethodAfterReturningAdvice<Double> {
        @Override
        public Double afterReturning(Method method, Object result, Object... args) {
            return ((Integer) result).doubleValue();
        }
    }

    public static class ResultConverter1 extends AbstractServiceMethodAfterReturningAdvice<Double> {
        @Override
        public Double afterReturning(Method method, Object result, Object... args) {
            return ((Double) result).doubleValue();
        }
    }

    public static class AroundInterceptor implements ServiceMethodAroundAdvice<Integer> {
        @Override
        public Integer around(Method interfaceMethod, Method beanMethod, Object proxy, Object... args) {
            return Integer.valueOf(-1);
        }

    }

    public static class BeforeConverterA implements ServiceMethodBeforeAdvice {

        @Override
        public void before(Method interfaceMethod, Object serviceTarget, Object... args) throws Throwable {
            args[1] = "A+" + vl2 + args[1];
        }

        @Value("${some.prop}")
        private String vl2;

        @PostConstruct
        public void init() {
            vl2 += "+@PostConstruct-";
        }
    }

    public static class BeforeConverterB implements ServiceMethodBeforeAdvice {

        @Override
        public void before(Method interfaceMethod, Object serviceTarget, Object... args) throws Throwable {
            args[1] = "B-" + args[1];
        }
    }

    public static class BeforeConverterC implements ServiceMethodBeforeAdvice {

        @Override
        public void before(Method interfaceMethod, Object serviceTarget, Object... args) throws Throwable {
            args[1] = "C-" + args[1];
        }
    }

    public static class BeforeConverterD implements ServiceMethodBeforeAdvice {

        @Override
        public void before(Method interfaceMethod, Object serviceTarget, Object... args) throws Throwable {
            args[1] = "D-" + args[1];
        }
    }

    public static class BeforeConverterE implements ServiceMethodBeforeAdvice {

        @Override
        public void before(Method interfaceMethod, Object serviceTarget, Object... args) throws Throwable {
            args[1] = "E-" + args[1];
        }
    }

    public static class AfterConverter1 extends AbstractServiceMethodAfterReturningAdvice<String> {

        @Override
        public String afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception {
            return result + "-1";
        }
    }

    public static class AfterConverter2 extends AbstractServiceMethodAfterReturningAdvice<String> {

        @Override
        public String afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception {
            return result + "-2";
        }
    }

    public static class AfterConverter3 extends AbstractServiceMethodAfterReturningAdvice<String> {

        @Override
        public String afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception {
            return result + "-3+" + vl2;
        }

        @Value("${some.prop}")
        private String vl2;

        @PostConstruct
        public void init() {
            vl2 += "+@PostConstruct";
        }

    }

    public static class AfterConverter4 extends AbstractServiceMethodAfterReturningAdvice<String> {

        @Override
        public String afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception {
            return result + "-4";
        }
    }

    public static class AfterConverter5 extends AbstractServiceMethodAfterReturningAdvice<String> {

        String methodName;

        @InjectOpenMember
        public void setIOpenMember(IOpenMember openMember) {
            this.methodName = openMember.getName().toUpperCase();
        }

        @Autowired
        IOpenMember openMethod;

        @Override
        public String afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception {
            return result + "-5: " + openMethod.getName() + " - @" + openMethod.getClass().getSimpleName() + " : " + methodName;
        }
    }


    public interface OverloadInterface {
        @ServiceCallAfterInterceptor(value = ResultConverter1.class)
        Double driverRiskScoreOverloadTest(IRulesRuntimeContext runtimeContext, String driverRisk);

        @ServiceCallAfterInterceptor(ResultConverter.class)
        @ServiceCallAroundInterceptor(AroundInterceptor.class)
        Double driverRiskScoreNoOverloadTest(IRulesRuntimeContext runtimeContext, String driverRisk);

        @ServiceExtraMethod(NonExistedMethodServiceExtraMethodHandler.class)
        Double nonExistedMethod(String driverRisk);

        @ServiceExtraMethod(LoadClassServiceExtraMethodHandler.class)
        Object loadClassMethod();

        @ServiceExtraMethod(InjectOpenLBeansExtraMethodHandler.class)
        String testSpring();

        @ServiceCallBeforeInterceptor(BeforeConverterA.class)
        @ServiceCallBeforeInterceptor({BeforeConverterB.class, BeforeConverterC.class})
        @ServiceCallBeforeInterceptor(BeforeConverterD.class)
        @ServiceCallAfterInterceptor(AfterConverter1.class)
        @ServiceCallAfterInterceptor({AfterConverter2.class, AfterConverter3.class})
        @ServiceCallAfterInterceptor(AfterConverter4.class)
        @ServiceCallBeforeInterceptor(BeforeConverterE.class)
        @ServiceCallAfterInterceptor(AfterConverter5.class)
        String convert(IRulesRuntimeContext runtimeContext, String text);
    }

    public static abstract class AOverload {
        @ServiceCallAfterInterceptor(value = ResultConverter1.class)
        public abstract Double driverRiskScoreOverloadTest(IRulesRuntimeContext runtimeContext, String driverRisk);

        @ServiceCallAfterInterceptor(ResultConverter.class)
        @ServiceCallAroundInterceptor(AroundInterceptor.class)
        abstract Double driverRiskScoreNoOverloadTest(IRulesRuntimeContext runtimeContext, String driverRisk);

        @ServiceExtraMethod(NonExistedMethodServiceExtraMethodHandler.class)
        public abstract Double nonExistedMethod(String driverRisk);

        @ServiceExtraMethod(LoadClassServiceExtraMethodHandler.class)
        public abstract Object loadClassMethod();
    }

    public static class Overload {
        @ServiceCallAfterInterceptor(value = ResultConverter1.class)
        public Double driverRiskScoreOverloadTest(IRulesRuntimeContext runtimeContext, String driverRisk) {
            return null;
        }

        @ServiceCallAfterInterceptor(ResultConverter.class)
        @ServiceCallAroundInterceptor(AroundInterceptor.class)
        public Double driverRiskScoreNoOverloadTest(IRulesRuntimeContext runtimeContext, String driverRisk) {
            return null;
        }

        @ServiceExtraMethod(NonExistedMethodServiceExtraMethodHandler.class)
        public Double nonExistedMethod(String driverRisk) {
            return null;
        }

        @ServiceExtraMethod(LoadClassServiceExtraMethodHandler.class)
        public Object loadClassMethod() {
            return null;
        }
    }

    public static class NonExistedMethodServiceExtraMethodHandler implements ServiceExtraMethodHandler<Double> {
        @Override
        public Double invoke(Method interfaceMethod, Object serviceBean, Object... args) {
            return 12345d;
        }
    }

    public static class LoadClassServiceExtraMethodHandler implements ServiceExtraMethodHandler<Object> {
        @Override
        public Object invoke(Method interfaceMethod, Object serviceBean, Object... args) throws Exception {
            Class<?> myBeanClass = Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass("org.openl.generated.beans.MyBean");
            return myBeanClass.getDeclaredConstructor().newInstance();
        }
    }

    public static class InjectOpenLBeansExtraMethodHandler implements ServiceExtraMethodHandler<Object> {

        @Resource
        ClassLoader classLoader;

        IOpenClass openClass;

        @Autowired(required = false)
        IOpenMember openMember;

        String prop;

        InjectOpenLBeansExtraMethodHandler(@Autowired IOpenClass openClass, @Value("${some.prop}") String prop) {
            this.prop = prop;
            this.openClass = openClass;

        }

        @Override
        public String invoke(Method interfaceMethod, Object serviceBean, Object... args) throws Exception {
            var myBeanClass = classLoader.loadClass("org.openl.generated.beans.MyBean").getSimpleName();
            return myBeanClass + "_" + prop + "_" + openClass + "_" + openMember;
        }
    }

    @Autowired
    private RuleServiceOpenLServiceInstantiationFactoryImpl instantiationFactory;

    private DeploymentDescription deploymentDescription;
    private List<Module> modules;

    private ServiceDescription.ServiceDescriptionBuilder serviceDescriptionBuilder() {
        return new ServiceDescription.ServiceDescriptionBuilder().setServiceClassName(OverloadInterface.class.getName())
                .setName("service")
                .setUrl("/")
                .setProvideRuntimeContext(true)
                .setDeployment(deploymentDescription)
                .setModules(modules)
                .setServicePath("service")
                .setResourceLoader(location -> null);
    }

    @BeforeEach
    public void before() throws Exception {
        CommonVersion version = new CommonVersionImpl(0, 0, 1);
        deploymentDescription = new DeploymentDescription("someDeploymentName", version);

        modules = new ArrayList<>();
        Module module = new Module();
        module.setName("Overload");
        modules.add(module);
        ProjectDescriptor projectDescriptor = new ProjectDescriptor();
        projectDescriptor.setName("service");
        projectDescriptor.setModules(modules);
        projectDescriptor
                .setProjectFolder(Paths.get("./test-resources/ServiceInterfaceMethodInterceptingTest").toAbsolutePath());
        module.setProject(projectDescriptor);
        module.setRulesRootPath(new PathEntry("Overload.xls"));

        var ruleServiceLoader = mock(RuleServiceLoader.class);
        assertNotNull(instantiationFactory);
        instantiationFactory.setRuleServiceLoader(ruleServiceLoader);

        when(ruleServiceLoader.resolveProject(deploymentDescription.getName(),
                deploymentDescription.getVersion(),
                projectDescriptor.getName())).thenReturn(projectDescriptor);
        Deployment deployment = mock(Deployment.class);
        List<IProject> projects = new ArrayList<>();
        AProject project = mock(AProject.class);
        projects.add(project);
        when(project.getName()).thenReturn("service");
        when(deployment.getProjects()).thenReturn(projects);
        when(deployment.getDeploymentName()).thenReturn(deploymentDescription.getName());
        when(deployment.getCommonVersion()).thenReturn(deploymentDescription.getVersion());
        when(ruleServiceLoader.getDeployment(eq(deploymentDescription.getName()),
                eq(deploymentDescription.getVersion()))).thenReturn(deployment);
        when(ruleServiceLoader
                .resolveProject(deploymentDescription.getName(), deploymentDescription.getVersion(), "service"))
                .thenReturn(projectDescriptor);
    }

    @Test
    public void testResultConverterInterceptor() throws Exception {
        OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder().build());
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, Calendar.JUNE, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        assertEquals(100, instance.driverRiskScoreOverloadTest(runtimeContext, "High Risk Driver"), 0.1);
    }

    @Test
    public void testInterceptors() throws Exception {
        OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder().build());
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        assertEquals("E-D-C-B-A+@Value+@PostConstruct-INPUT_-1-2-3+@Value+@PostConstruct-4-5: convert - @SpreadsheetWrapper : CONVERT", instance.convert(runtimeContext, "INPUT"));
    }

    @Test
    public void testSpringAnnotations() throws Exception {
        OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder().build());
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        assertEquals("MyBean_@Value_VirtualModule_null", instance.testSpring());
    }

    @Test
    public void testResultConverterInterceptor2() throws Exception {
        OpenLService service = instantiationFactory
                .createService(serviceDescriptionBuilder().setAnnotationTemplateClassName(AOverload.class.getName())
                        .setServiceClassName(null)
                        .build());
        Object serviceBean = service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, Calendar.JUNE, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        Method method = serviceBean.getClass()
                .getDeclaredMethod("driverRiskScoreOverloadTest", IRulesRuntimeContext.class, String.class);
        assertEquals(100, (Double) method.invoke(serviceBean, runtimeContext, "High Risk Driver"), 0.1);
    }

    @Test
    public void testNonExistedInRulesMethod() throws Exception {
        OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder().build());
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, Calendar.JUNE, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        assertEquals(12345, instance.nonExistedMethod("High Risk Driver"), 0.1);
    }

    @Test
    public void testInterceptorClassloader() throws Exception {
        OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder().build());
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, Calendar.JUNE, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        assertNotNull(instance.loadClassMethod());
        assertEquals("org.openl.generated.beans.MyBean", instance.loadClassMethod().getClass().getName());
    }

    @Test
    public void testGroupAndAroundInterceptor2() throws Exception {
        OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder().build());
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, Calendar.JUNE, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        Double result = instance.driverRiskScoreNoOverloadTest(runtimeContext, "High Risk Driver");
        assertEquals(-1d, result, 0.1d);
    }

    @Test
    public void testServiceClassUndecorating() throws Exception {
        ServiceDescription serviceDescription = serviceDescriptionBuilder().build();
        Class<?> interfaceForInstantiationStrategy = RuleServiceInstantiationFactoryHelper
                .buildInterfaceForInstantiationStrategy(OverloadInterface.class,
                        Thread.currentThread().getContextClassLoader(),
                        null,
                        serviceDescription.isProvideRuntimeContext());
        assertEquals(3, interfaceForInstantiationStrategy.getMethods().length);
        assertEquals(String.class, interfaceForInstantiationStrategy.getMethod("convert", IRulesRuntimeContext.class, String.class).getReturnType());
        assertEquals(Object.class, interfaceForInstantiationStrategy.getMethod("driverRiskScoreOverloadTest", IRulesRuntimeContext.class, String.class).getReturnType());
        assertEquals(Object.class, interfaceForInstantiationStrategy.getMethod("driverRiskScoreNoOverloadTest", IRulesRuntimeContext.class, String.class).getReturnType());
    }

    @Test
    public void testMissingInterfaceClasses() {
        try {
            OpenLService service = instantiationFactory
                    .createService(serviceDescriptionBuilder().setServiceClassName(ELUSIVE_CLASS_NAME).build());
            service.getServiceClass();
            fail("Everything went different before...");
        } catch (RuleServiceInstantiationException e) {
            Throwable actual = findExceptionByMessage(e,
                    "Failed to load a service class 'org.openl.test.MustNotBeFoundInClassloader'.");
            assertNotNull(actual, "Exception must be present");
            assertTrue(actual.getCause() instanceof ClassNotFoundException);
        }
        try {
            OpenLService service = instantiationFactory
                    .createService(serviceDescriptionBuilder().setServiceClassName(null)
                            .setAnnotationTemplateClassName(ELUSIVE_CLASS_NAME)
                            .build());
            service.getServiceClass();
            fail("Everything went different before...");
        } catch (RuleServiceInstantiationException e) {
            Throwable actual = findExceptionByMessage(e,
                    "Failed to load or apply annotation template class 'org.openl.test.MustNotBeFoundInClassloader'.");
            assertNotNull(actual, "Exception must be present");
            assertTrue(actual.getCause() instanceof ClassNotFoundException);
        }

        try {
            OpenLService service = instantiationFactory
                    .createService(serviceDescriptionBuilder().setServiceClassName(null)
                            .setAnnotationTemplateClassName(Overload.class.getName())
                            .build());
            service.getServiceClass();
            fail("Everything went different before...");
        } catch (RuleServiceInstantiationException e) {
            Throwable actual = findExceptionByMessage(e,
                    "Failed to apply annotation template class 'org.openl.rules.ruleservice.core.interceptors.ServiceInterfaceMethodInterceptingTest$Overload'. Interface or abstract class is expected, but class is found.");
            assertNotNull(actual, "Exception must be present");
        }
        try {
            OpenLService service = instantiationFactory
                    .createService(serviceDescriptionBuilder().setServiceClassName(AOverload.class.getName()).build());
            service.getServiceClass();
            fail("Everything went different before...");
        } catch (RuleServiceInstantiationException e) {
            Throwable actual = findExceptionByMessage(e,
                    "Failed to apply service class 'class org.openl.rules.ruleservice.core.interceptors.ServiceInterfaceMethodInterceptingTest$AOverload'. Interface is expected, but class is found.");
            assertNotNull(actual, "Exception must be present");
        }
    }

    private static Throwable findExceptionByMessage(Exception e, String expectedMessage) {
        Throwable it = e;
        while (it != null && !Objects.equals(expectedMessage, it.getMessage())) {
            it = it.getCause();
        }
        return it;
    }

}
