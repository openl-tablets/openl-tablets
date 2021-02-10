package org.openl.rules.ruleservice.core.interceptors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.dependency.IDependencyManager;
import org.openl.meta.DoubleValue;
import org.openl.meta.IntValue;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.abstraction.IProject;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.SimpleDependencyManager;
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
import org.openl.rules.ruleservice.loader.RuleServiceLoader;

public class ServiceInterfaceMethodInterceptingTest {

    private static final String ELUSIVE_CLASS_NAME = "org.openl.test.MustNotBeFoundInClassloader";

    public static class ResultConverter extends AbstractServiceMethodAfterReturningAdvice<Double> {
        @Override
        public Double afterReturning(Method method, Object result, Object... args) {
            return ((IntValue) result).doubleValue();
        }
    }

    public static class ResultConverter1 extends AbstractServiceMethodAfterReturningAdvice<Double> {
        @Override
        public Double afterReturning(Method method, Object result, Object... args) {
            return ((DoubleValue) result).doubleValue();
        }
    }

    public static class AroundInterceptor implements ServiceMethodAroundAdvice<IntValue> {
        @Override
        public IntValue around(Method interfaceMethod, Method beanMethod, Object proxy, Object... args) {
            return new IntValue(-1);
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
            return myBeanClass.newInstance();
        }
    }

    private RuleServiceLoader ruleServiceLoader;
    private DeploymentDescription deploymentDescription;
    private List<Module> modules;

    private ServiceDescription.ServiceDescriptionBuilder serviceDescriptionBuilder() {
        return new ServiceDescription.ServiceDescriptionBuilder()
                .setServiceClassName(OverloadInterface.class.getName())
                .setName("service")
                .setUrl("/")
                .setProvideRuntimeContext(true)
                .setProvideVariations(false)
                .setDeployment(deploymentDescription)
                .setModules(modules)
                .setResourceLoader(location -> null);
    }

    @Before
    public void before() {
        CommonVersion version = new CommonVersionImpl(0, 0, 1);
        deploymentDescription = new DeploymentDescription("someDeploymentName", version);

        modules = new ArrayList<>();
        Module module = new Module();
        module.setName("Overload");
        modules.add(module);
        ProjectDescriptor projectDescriptor = new ProjectDescriptor();
        projectDescriptor.setName("service");
        projectDescriptor.setModules(modules);
        projectDescriptor.setProjectFolder(Paths.get("./test-resources/ServiceInterfaceMethodInterceptingTest").toAbsolutePath());
        module.setProject(projectDescriptor);
        module.setRulesRootPath(new PathEntry("Overload.xls"));

        ruleServiceLoader = mock(RuleServiceLoader.class);

        when(ruleServiceLoader.resolveModulesForProject(deploymentDescription.getName(),
            deploymentDescription.getVersion(),
            projectDescriptor.getName())).thenReturn(modules);
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
            .resolveModulesForProject(deploymentDescription.getName(), deploymentDescription.getVersion(), "service"))
                .thenReturn(modules);
    }

    @Test
    public void testResultConverterInterceptor() throws Exception {
        RuleServiceOpenLServiceInstantiationFactoryImpl instantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        instantiationFactory.setRuleServiceLoader(ruleServiceLoader);
        OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder().build());
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, Calendar.JUNE, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        Assert.assertEquals(100, instance.driverRiskScoreOverloadTest(runtimeContext, "High Risk Driver"), 0.1);
    }

    @Test
    public void testResultConverterInterceptor2() throws Exception {
        RuleServiceOpenLServiceInstantiationFactoryImpl instantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        instantiationFactory.setRuleServiceLoader(ruleServiceLoader);
        OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder()
                .setAnnotationTemplateClassName(AOverload.class.getName())
                .setServiceClassName(null)
                .build());
        Object serviceBean = service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, Calendar.JUNE, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        Method method = serviceBean.getClass().getDeclaredMethod("driverRiskScoreOverloadTest", IRulesRuntimeContext.class, String.class);
        Assert.assertEquals(100, (Double) method.invoke(serviceBean, runtimeContext, "High Risk Driver"), 0.1);
    }

    @Test
    public void testNonExistedInRulesMethod() throws Exception {
        RuleServiceOpenLServiceInstantiationFactoryImpl instantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        instantiationFactory.setRuleServiceLoader(ruleServiceLoader);
        OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder().build());
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, Calendar.JUNE, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        Assert.assertEquals(12345, instance.nonExistedMethod("High Risk Driver"), 0.1);
    }

    @Test
    public void testInterceptorClassloader() throws Exception {
        RuleServiceOpenLServiceInstantiationFactoryImpl instantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        instantiationFactory.setRuleServiceLoader(ruleServiceLoader);
        OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder().build());
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, Calendar.JUNE, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        assertNotNull(instance.loadClassMethod());
    }

    @Test
    public void testGroupAndAroundInterceptor2() throws Exception {
        RuleServiceOpenLServiceInstantiationFactoryImpl instantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        instantiationFactory.setRuleServiceLoader(ruleServiceLoader);
        OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder().build());
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, Calendar.JUNE, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        Double result = instance.driverRiskScoreNoOverloadTest(runtimeContext, "High Risk Driver");
        Assert.assertEquals(-1d, result, 0.1d);
    }

    @Test
    public void testServiceClassUndecorating() throws Exception {
        ServiceDescription serviceDescription = serviceDescriptionBuilder().build();
        RuleServiceOpenLServiceInstantiationFactoryImpl instantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        instantiationFactory.setRuleServiceLoader(ruleServiceLoader);
        IDependencyManager dependencyManager = new SimpleDependencyManager(Collections
            .emptyList(), null, false, true, null);
        RulesInstantiationStrategy rulesInstantiationStrategy = instantiationFactory.getInstantiationStrategyFactory()
            .getStrategy(serviceDescription, dependencyManager);
        Class<?> interfaceForInstantiationStrategy = RuleServiceInstantiationFactoryHelper
            .buildInterfaceForInstantiationStrategy(OverloadInterface.class,
                rulesInstantiationStrategy.getClassLoader(),
                serviceDescription.isProvideRuntimeContext(),
                serviceDescription.isProvideVariations());
        for (Method method : OverloadInterface.class.getMethods()) {
            if (!method.isAnnotationPresent(ServiceExtraMethod.class)) {
                Method methodGenerated = interfaceForInstantiationStrategy.getMethod(method.getName(),
                    method.getParameterTypes());
                assertNotNull(methodGenerated);
                Assert.assertEquals(Object.class, methodGenerated.getReturnType());
            }
        }
    }

    @Test
    public void testMissingInterfaceClasses() {
        RuleServiceOpenLServiceInstantiationFactoryImpl instantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        instantiationFactory.setRuleServiceLoader(ruleServiceLoader);
        try {
            OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder().setServiceClassName(ELUSIVE_CLASS_NAME).build());
            service.getServiceClass();
            fail("Everything went different before...");
        } catch (RuleServiceInstantiationException e) {
            Throwable actual = findExceptionByMessage(e, "Failed to load a service class 'org.openl.test.MustNotBeFoundInClassloader'.");
            assertNotNull("Exception must be present", actual);
            assertTrue(actual.getCause() instanceof ClassNotFoundException);
        }
        try {
            OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder()
                    .setServiceClassName(null)
                    .setAnnotationTemplateClassName(ELUSIVE_CLASS_NAME)
                    .build());
            service.getServiceClass();
            fail("Everything went different before...");
        } catch (RuleServiceInstantiationException e) {
            Throwable actual = findExceptionByMessage(e, "Failed to load or apply annotation template class 'org.openl.test.MustNotBeFoundInClassloader'.");
            assertNotNull("Exception must be present", actual);
            assertTrue(actual.getCause() instanceof ClassNotFoundException);
        }

        try {
            OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder()
                    .setServiceClassName(null)
                    .setRmiServiceClassName(ELUSIVE_CLASS_NAME)
                    .setPublishers(new String[]{"RMI"})
                    .build());
            service.getServiceClass();
            fail("Everything went different before...");
        } catch (RuleServiceInstantiationException e) {
            Throwable actual = findExceptionByMessage(e, "Failed to load RMI service class 'org.openl.test.MustNotBeFoundInClassloader'.");
            assertNotNull("Exception must be present", actual);
            assertTrue(actual.getCause() instanceof ClassNotFoundException);
        }

        try {
            OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder()
                    .setServiceClassName(null)
                    .setAnnotationTemplateClassName(Overload.class.getName())
                    .build());
            service.getServiceClass();
            fail("Everything went different before...");
        } catch (RuleServiceInstantiationException e) {
            Throwable actual = findExceptionByMessage(e, "Failed to apply annotation template class 'org.openl.rules.ruleservice.core.interceptors.ServiceInterfaceMethodInterceptingTest$Overload'. Interface or abstract class is expected, but class is found.");
            assertNotNull("Exception must be present", actual);
        }
        try {
            OpenLService service = instantiationFactory.createService(serviceDescriptionBuilder()
                    .setServiceClassName(AOverload.class.getName())
                    .build());
            service.getServiceClass();
            fail("Everything went different before...");
        } catch (RuleServiceInstantiationException e) {
            Throwable actual = findExceptionByMessage(e, "Failed to apply service class 'class org.openl.rules.ruleservice.core.interceptors.ServiceInterfaceMethodInterceptingTest$AOverload'. Interface is expected, but class is found.");
            assertNotNull("Exception must be present", actual);
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
