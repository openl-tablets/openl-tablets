package org.openl.rules.ruleservice.core.interceptors;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ruleservice.core.*;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptors;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAroundInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallInterceptorGroup;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;

public class ServiceInterfaceMethodInterceptingTest {
    public static class ResultConvertor extends AbstractServiceMethodAfterReturningAdvice<Double> {
        @Override
        public Double afterReturning(Method method, Object result, Object... args) throws Exception {
            return ((DoubleValue) result).doubleValue();
        }
    }

    public static class AroundInterceptor implements ServiceMethodAroundAdvice<DoubleValue> {
        @Override
        public DoubleValue around(Method interfacemethod,
                Method beanMethod,
                Object proxy,
                Object... args) throws Exception {
            return new DoubleValue(-1);
        }

    }

    public interface OverloadInterface {
        @ServiceCallAfterInterceptor(value = ResultConvertor.class, group = ServiceCallInterceptorGroup.GROUP1)
        Double driverRiskScoreOverloadTest(IRulesRuntimeContext runtimeContext, String driverRisk);

        @ServiceCallAfterInterceptors({
                @ServiceCallAfterInterceptor(value = ResultConvertor.class, group = ServiceCallInterceptorGroup.ALL) })
        @ServiceCallAroundInterceptor(value = AroundInterceptor.class)
        Double driverRiskScoreNoOverloadTest(IRulesRuntimeContext runtimeContext, String driverRisk);

        @ServiceExtraMethod(NonExistedMethodServiceExtraMethodHandler.class)
        Double nonExistedMethod(String driverRisk);

        @ServiceExtraMethod(LoadClassServiceExtraMethodHandler.class)
        Object loadClassMethod();
    }

    public static class NonExistedMethodServiceExtraMethodHandler implements ServiceExtraMethodHandler<Double> {
        @Override
        public Double invoke(Method interfaceMethod, Object serviceBean, Object... args) throws Exception {
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

    ServiceDescription serviceDescription;
    RuleServiceLoader ruleServiceLoader;
    List<Module> modules = new ArrayList<>();

    @Before
    public void before() {
        CommonVersion version = new CommonVersionImpl(0, 0, 1);
        DeploymentDescription deploymentDescription = new DeploymentDescription("someDeploymentName", version);

        Module module = new Module();
        module.setName("Overload");
        ProjectDescriptor projectDescriptor = new ProjectDescriptor();
        projectDescriptor.setName("service");
        projectDescriptor.setModules(modules);
        module.setProject(projectDescriptor);
        module.setRulesRootPath(new PathEntry("./test-resources/ServiceInterfaceMethodInterceptingTest/Overload.xls"));
        modules.add(module);

        serviceDescription = new ServiceDescription.ServiceDescriptionBuilder()
            .setServiceClassName(OverloadInterface.class.getName())
            .setName("service")
            .setUrl("/")
            .setProvideRuntimeContext(true)
            .setProvideVariations(false)
            .setDeployment(deploymentDescription)
            .setModules(modules)
            .build();

        ruleServiceLoader = mock(RuleServiceLoader.class);

        when(ruleServiceLoader.resolveModulesForProject(deploymentDescription.getName(),
            deploymentDescription.getVersion(),
            projectDescriptor.getName())).thenReturn(modules);
        List<Deployment> deployments = new ArrayList<>();
        Deployment deployment = mock(Deployment.class);
        List<AProject> projects = new ArrayList<>();
        AProject project = mock(AProject.class);
        projects.add(project);
        when(project.getName()).thenReturn("service");
        when(deployment.getProjects()).thenReturn(projects);
        when(deployment.getDeploymentName()).thenReturn(deploymentDescription.getName());
        when(deployment.getCommonVersion()).thenReturn(deploymentDescription.getVersion());
        deployments.add(deployment);
        when(ruleServiceLoader.getDeployments()).thenReturn(deployments);
        when(ruleServiceLoader
            .resolveModulesForProject(deploymentDescription.getName(), deploymentDescription.getVersion(), "service"))
                .thenReturn(modules);
    }

    @Test
    public void testResultConvertorInterceptor() throws Exception {
        RuleServiceOpenLServiceInstantiationFactoryImpl instantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        instantiationFactory
            .setServiceCallInterceptorGroups(new ServiceCallInterceptorGroup[] { ServiceCallInterceptorGroup.GROUP1 });
        instantiationFactory.setRuleServiceLoader(ruleServiceLoader);
        OpenLService service = instantiationFactory.createService(serviceDescription);
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, 5, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        Assert.assertEquals(100, instance.driverRiskScoreOverloadTest(runtimeContext, "High Risk Driver"), 0.1);
    }

    @Test
    public void testNonExistedInRulesMethod() throws Exception {
        RuleServiceOpenLServiceInstantiationFactoryImpl instantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        instantiationFactory
            .setServiceCallInterceptorGroups(new ServiceCallInterceptorGroup[] { ServiceCallInterceptorGroup.GROUP1 });
        instantiationFactory.setRuleServiceLoader(ruleServiceLoader);
        OpenLService service = instantiationFactory.createService(serviceDescription);
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, 5, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        Assert.assertEquals(12345, instance.nonExistedMethod("High Risk Driver"), 0.1);
    }

    @Test
    public void testInterceptorClassloader() throws Exception {
        RuleServiceOpenLServiceInstantiationFactoryImpl instantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        instantiationFactory
            .setServiceCallInterceptorGroups(new ServiceCallInterceptorGroup[] { ServiceCallInterceptorGroup.GROUP1 });
        instantiationFactory.setRuleServiceLoader(ruleServiceLoader);
        OpenLService service = instantiationFactory.createService(serviceDescription);
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, 5, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        Assert.assertNotNull(instance.loadClassMethod());
    }

    @Test(expected = ClassCastException.class)
    public void testGroupInterceptor1() throws Exception {
        RuleServiceOpenLServiceInstantiationFactoryImpl instantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        instantiationFactory
            .setServiceCallInterceptorGroups(new ServiceCallInterceptorGroup[] { ServiceCallInterceptorGroup.GROUP2 });
        instantiationFactory.setRuleServiceLoader(ruleServiceLoader);
        OpenLService service = instantiationFactory.createService(serviceDescription);
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, 5, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        instance.driverRiskScoreOverloadTest(runtimeContext, "High Risk Driver");
    }

    @Test
    public void testGroupAndAroundInterceptor2() throws Exception {
        RuleServiceOpenLServiceInstantiationFactoryImpl instantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        instantiationFactory
            .setServiceCallInterceptorGroups(new ServiceCallInterceptorGroup[] { ServiceCallInterceptorGroup.GROUP2 });
        instantiationFactory.setRuleServiceLoader(ruleServiceLoader);
        OpenLService service = instantiationFactory.createService(serviceDescription);
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, 5, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        Double result = instance.driverRiskScoreNoOverloadTest(runtimeContext, "High Risk Driver");
        Assert.assertEquals(new Double(-1), result, 0.1d);
    }

    @Test
    public void testServiceClassUndecorating() throws Exception {
        RuleServiceOpenLServiceInstantiationFactoryImpl instantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        instantiationFactory.setRuleServiceLoader(ruleServiceLoader);
        Class<?> interfaceForInstantiationStrategy = RuleServiceInstantiationFactoryHelper
            .getInterfaceForInstantiationStrategy(
                instantiationFactory.getInstantiationStrategyFactory().getStrategy(serviceDescription, null),
                OverloadInterface.class);
        for (Method method : OverloadInterface.class.getMethods()) {
            if (!method.isAnnotationPresent(ServiceExtraMethod.class)) {
                Method methodGenerated = interfaceForInstantiationStrategy.getMethod(method.getName(),
                    method.getParameterTypes());
                Assert.assertNotNull(methodGenerated);
                Assert.assertEquals(Object.class, methodGenerated.getReturnType());
            }
        }
    }

}
