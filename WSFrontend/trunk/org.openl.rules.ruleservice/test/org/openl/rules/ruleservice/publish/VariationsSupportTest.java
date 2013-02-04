package org.openl.rules.ruleservice.publish;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceOpenLServiceInstantiationFactoryImpl;
import org.openl.rules.ruleservice.simple.JavaClassRuleServicePublisher;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.openl.rules.ruleservice.simple.RulesFrontendImpl;
import org.openl.rules.variation.JXPathVariation;
import org.openl.rules.variation.NoVariation;
import org.openl.rules.variation.VariationsPack;
import org.openl.rules.variation.VariationsResult;

public class VariationsSupportTest {
    public static final String STANDART = "Standard Driver";
    public static final String YOUNG = "Young Driver";
    public static final String SENOIR = "Senior Driver";
    private static JavaClassRuleServicePublisher publisher;
    private static RulesFrontend frontend;
    private static RulesProjectResolver resolver;
    private static RuleServiceOpenLServiceInstantiationFactoryImpl ruleServiceOpenLServiceInstantiationFactory;

    private static OpenLService service1;


    @BeforeClass
    public static void init() throws Exception {
        resolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        frontend = new RulesFrontendImpl();
        publisher = new JavaClassRuleServicePublisher();
        publisher.setFrontend(frontend);
        ruleServiceOpenLServiceInstantiationFactory = new RuleServiceOpenLServiceInstantiationFactoryImpl();

        File tut4Folder = new File("./test-resources/org.openl.tablets.tutorial4");
        ResolvingStrategy tut4ResolvingStrategy = resolver.isRulesProject(tut4Folder);
        assertNotNull(tut4ResolvingStrategy);
        service1 = ruleServiceOpenLServiceInstantiationFactory.createOpenLService("tutorial4",
            "no_url",
            "org.openl.rules.tutorial4.Tutorial4WithVariations",
            false, true,
            tut4ResolvingStrategy.resolveProject(tut4Folder).getModules());
    }

    @Before
    public void before() throws Exception {
        publisher.undeploy(service1.getName());
        publisher.deploy(service1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testVariations() throws Exception {
        Object driver = publisher.getServiceByName("tutorial4")
            .getServiceClass()
            .getClassLoader()
            .loadClass("org.openl.generated.beans.publisher.test.Driver")
            .newInstance();
        Method nameSetter = driver.getClass().getMethod("setGender", String.class);
        nameSetter.invoke(driver, "Male");
        Method ageSetter = driver.getClass().getMethod("setAge", int.class);
        ageSetter.invoke(driver, 40);
        VariationsPack variations = new VariationsPack(new JXPathVariation("young", 0, "age", 18), new JXPathVariation("senior", 0, "age", 71));
        VariationsResult<String> resultsDrivers = (VariationsResult<String>) frontend.execute("tutorial4", "driverAgeType", new Object[] { driver , variations});
        assertEquals(resultsDrivers.getResultForVariation("young"), YOUNG);
        assertEquals(resultsDrivers.getResultForVariation("senior"), SENOIR);
        assertEquals(resultsDrivers.getResultForVariation(NoVariation.ORIGINAL_CALCULATION), STANDART);
        assertTrue(resultsDrivers.getVariationFailures().isEmpty());
    }
}
