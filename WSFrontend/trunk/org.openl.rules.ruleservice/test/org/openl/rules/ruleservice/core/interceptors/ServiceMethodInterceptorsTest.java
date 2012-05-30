package org.openl.rules.ruleservice.core.interceptors;

import static junit.framework.Assert.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.List;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationFactoryHelper;
import org.openl.rules.ruleservice.core.RuleServiceOpenLServiceInstantiationFactoryImpl;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.publish.RuleServiceInstantiationStrategyFactoryImpl;

public class ServiceMethodInterceptorsTest {
    private static RulesProjectResolver projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();

    public static class ResultConvertor extends AbstractServiceMethodAfterReturningAdvice<Double> {

        @Override
        public Double afterReturning(Method method, Object result, Object... args) throws Exception {
            return ((DoubleValue) result).doubleValue();
        }

    }

    public static interface OverloadInterface {
        @ServiceCallAfterInterceptor(value = ResultConvertor.class)
        Double driverRiskScoreOverloadTest(IRulesRuntimeContext runtimeContext, String driverRisk);
        @Deprecated
        @ServiceCallAfterInterceptor(value = ResultConvertor.class)
        Double driverRiskScoreNoOverloadTest(IRulesRuntimeContext runtimeContext, String driverRisk);
    }

    @Test
    public void testResultConvertorInterceptor() throws Exception {
        File folder = new File("./test-resources/filesystemdatasource/simple_project/");
        List<Module> modules = projectResolver.isRulesProject(folder).resolveProject(folder).getModules();
        RuleServiceOpenLServiceInstantiationFactoryImpl instantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();
        OpenLService service = instantiationFactory.createOpenLService("test",
            "/",
            OverloadInterface.class.getName(),
            true,
            modules);
        assertTrue(service.getServiceBean() instanceof OverloadInterface);
        OverloadInterface instance = (OverloadInterface) service.getServiceBean();
        IRulesRuntimeContext runtimeContext = new DefaultRulesRuntimeContext();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, 5, 15);
        runtimeContext.setCurrentDate(calendar.getTime());
        System.out.println(instance.driverRiskScoreOverloadTest(runtimeContext, ""));
    }
    
    @Test
    public void testServiceClassUndecorating() throws Exception {
        File folder = new File("./test-resources/filesystemdatasource/simple_project/");
        List<Module> modules = projectResolver.isRulesProject(folder).resolveProject(folder).getModules();
        RulesInstantiationStrategy instantiationStrategy = new RuleServiceInstantiationStrategyFactoryImpl().getStrategy(modules, null);
        Class<?> interfaceForInstantiationStrategy = RuleServiceInstantiationFactoryHelper.getInterfaceForInstantiationStrategy(instantiationStrategy, OverloadInterface.class);
        for(Method method: OverloadInterface.class.getMethods()){
            Method methodGenerated = interfaceForInstantiationStrategy.getMethod(method.getName(), method.getParameterTypes());
            assertNotNull(methodGenerated);
            assertEquals(Object.class, methodGenerated.getReturnType());
        }
    }
    
}
