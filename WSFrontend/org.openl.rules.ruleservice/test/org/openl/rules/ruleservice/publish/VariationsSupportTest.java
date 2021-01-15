package org.openl.rules.ruleservice.publish;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.openl.rules.variation.JXPathVariation;
import org.openl.rules.variation.NoVariation;
import org.openl.rules.variation.VariationsPack;
import org.openl.rules.variation.VariationsResult;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "ruleservice.datasource.dir=test-resources/VariationsSupportTest",
        "ruleservice.isProvideRuntimeContext=false",
        "ruleservice.isSupportVariations=true" })
@ContextConfiguration({ "classpath:openl-ruleservice-beans.xml" })
public class VariationsSupportTest implements ApplicationContextAware {
    public static final String STANDARD = "Standard Driver";
    public static final String YOUNG = "Young Driver";
    public static final String SENIOR = "Senior Driver";

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testVariations() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);

        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        Object driver = serviceManager.getServiceByName("org.openl.tablets.tutorial4")
            .getServiceClass()
            .getClassLoader()
            .loadClass("org.openl.generated.beans.publisher.test.Driver")
            .newInstance();
        Method nameSetter = driver.getClass().getMethod("setGender", String.class);
        nameSetter.invoke(driver, "Male");
        Method ageSetter = driver.getClass().getMethod("setAge", int.class);
        ageSetter.invoke(driver, 40);
        VariationsPack variations = new VariationsPack(new JXPathVariation("young", 0, "age", 18),
            new JXPathVariation("senior", 0, "age", 71));
        VariationsResult<String> resultsDrivers = (VariationsResult<String>) frontend.execute(
            "org.openl.rules.tutorial4.Tutorial4WithVariations",
            "driverAgeType",
            new Object[] { driver, variations });
        assertEquals(YOUNG, resultsDrivers.getResultForVariation("young"));
        assertEquals(SENIOR, resultsDrivers.getResultForVariation("senior"));
        assertEquals(STANDARD, resultsDrivers.getResultForVariation(NoVariation.ORIGINAL_CALCULATION));
        assertTrue(resultsDrivers.getVariationFailures().isEmpty());
    }
}
