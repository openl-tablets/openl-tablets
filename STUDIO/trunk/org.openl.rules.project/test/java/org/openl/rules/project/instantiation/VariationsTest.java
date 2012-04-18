package org.openl.rules.project.instantiation;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.openl.generated.beans.Driver;
import org.openl.generated.beans.Policy;
import org.openl.meta.DoubleValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.project.instantiation.variation.ArgumentReplacementVariation;
import org.openl.rules.project.instantiation.variation.FieldModificationVariation;
import org.openl.rules.project.instantiation.variation.JXPathVariation;
import org.openl.rules.project.instantiation.variation.NoVariation;
import org.openl.rules.project.instantiation.variation.VariationsEnhancer;
import org.openl.rules.project.instantiation.variation.VariationsEnhancerHelper;
import org.openl.rules.project.instantiation.variation.VariationsPack;
import org.openl.rules.project.instantiation.variation.VariationsResult;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.RulesProjectResolver;

@SuppressWarnings("unchecked")
public class VariationsTest {
    private static final String TEST_PROJECT_FOLDER = "test/resources/dependencies/test4/module/dependency-module1";
    public static final String STANDART = "Standard Driver";
    public static final String YOUNG = "Young Driver";
    public static final String SENOIR = "Senior Driver";
    private RulesProjectResolver projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();;
    private WrapperAdjustingInstantiationStrategy instantiationStrategy;

    @Before
    public void init() throws Exception {
        File tut4Folder = new File(TEST_PROJECT_FOLDER);
        ProjectDescriptor project = projectResolver.isRulesProject(tut4Folder).resolveProject(tut4Folder);
        instantiationStrategy = new WrapperAdjustingInstantiationStrategy(project.getModules().get(0), true, null);
    }

    @Test
    public void testEnhancement() throws Exception {
        // without interface
        VariationsEnhancer variationsEnhancer = new VariationsEnhancer(instantiationStrategy);
        assertTrue(VariationsEnhancerHelper.isEnhancedClass(variationsEnhancer.getInstanceClass()));
        assertFalse(VariationsEnhancerHelper.isEnhancedClass(instantiationStrategy.getInstanceClass()));

        // with correct interface
        VariationsEnhancer variationsEnhancerWithInterface = new VariationsEnhancer(instantiationStrategy);
        variationsEnhancerWithInterface.setServiceClass(EnhancedInterface.class);
        assertTrue(VariationsEnhancerHelper.isEnhancedClass(variationsEnhancerWithInterface.getInstanceClass()));
        assertFalse(VariationsEnhancerHelper.isEnhancedClass(instantiationStrategy.getInstanceClass()));

        // with wrong interface
        // we use simple api instantiation strategy because wrapper
        // instantiation strategy always has service class that can not be
        // modified
        File folder = new File(new File(TEST_PROJECT_FOLDER, "rules"), "main");
        ProjectDescriptor project = projectResolver.isRulesProject(folder).resolveProject(folder);
        ApiBasedInstantiationStrategy instantiationStrategy = new ApiBasedInstantiationStrategy(project.getModules()
            .get(0), true, null);
        VariationsEnhancer variationsEnhancerWithWrongInterface = new VariationsEnhancer(instantiationStrategy);
        variationsEnhancerWithWrongInterface.setServiceClass(WrongEnhancedInterface.class);
        try {
            variationsEnhancerWithWrongInterface.instantiate();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Failed to find corresrponding method in original class for method"));
        }
    }

    @Test
    public void testJXPathVariation() throws Exception {
        VariationsEnhancer variationsEnhancer = new VariationsEnhancer(instantiationStrategy);
        variationsEnhancer.setServiceClass(EnhancedInterface.class);
        EnhancedInterface instance = (EnhancedInterface) variationsEnhancer.instantiate();
        Driver[] drivers = instance.getDriverProfiles1();
        VariationsResult<String> resultsDrivers = instance.driverAgeType(drivers[0],
            new VariationsPack(new JXPathVariation("young", 0, "age", 18), new JXPathVariation("senior", 0, "age", 71)));
        assertTrue(resultsDrivers.getVariationFailures().isEmpty());
        assertEquals(resultsDrivers.getResultForVariation("young"), YOUNG);
        assertEquals(resultsDrivers.getResultForVariation("senior"), SENOIR);
        assertEquals(resultsDrivers.getResultForVariation(NoVariation.ORIGIANAL_CALCULATION), STANDART);
        Policy[] policies = instance.getPolicyProfile1();
        VariationsResult<SpreadsheetResult> resultsPolicies = instance.processPolicy(policies[0],
            new VariationsPack(new JXPathVariation("young", 0, "drivers[name = 'Sara']/age", 17),
                new JXPathVariation("senior", 0, "drivers[name = 'Sara']/age", 88)));
        assertTrue(resultsPolicies.getVariationFailures().isEmpty());
        assertEquals(resultsPolicies.getResultForVariation("young").getFieldValue("$Value$Premium"), new DoubleValue(1390));
        assertEquals(resultsPolicies.getResultForVariation("senior").getFieldValue("$Value$Premium"), new DoubleValue(1290));
        assertEquals(resultsPolicies.getResultForVariation(NoVariation.ORIGIANAL_CALCULATION).getFieldValue("$Value$Premium"),
            new DoubleValue(1090));
    }

    @Test
    public void testReflectionVariation() throws Exception {
        VariationsEnhancer variationsEnhancer = new VariationsEnhancer(instantiationStrategy);
        variationsEnhancer.setServiceClass(EnhancedInterface.class);
        EnhancedInterface instance = (EnhancedInterface) variationsEnhancer.instantiate();
        Driver[] drivers = instance.getDriverProfiles1();
        VariationsResult<String> resultsDrivers = instance.driverAgeType(drivers[0],
            new VariationsPack(new FieldModificationVariation("young", 0, "age", 10),
                new FieldModificationVariation("senior", 0, "age", 71)));
        assertTrue(resultsDrivers.getVariationFailures().isEmpty());
        assertEquals(resultsDrivers.getResultForVariation("young"), YOUNG);
        assertEquals(resultsDrivers.getResultForVariation("senior"), SENOIR);
        assertEquals(resultsDrivers.getResultForVariation(NoVariation.ORIGIANAL_CALCULATION), STANDART);
        Policy[] policies = instance.getPolicyProfile1();
        VariationsResult<SpreadsheetResult> resultsPolicies = instance.processPolicy(policies[0],
            new VariationsPack(new FieldModificationVariation("young", 0, "drivers/0/age", 17), new FieldModificationVariation("senior",
                0,
                "drivers/0/age",
                88)));
        assertTrue(resultsPolicies.getVariationFailures().isEmpty());
        assertEquals(resultsPolicies.getResultForVariation("young").getFieldValue("$Value$Premium"), new DoubleValue(1390));
        assertEquals(resultsPolicies.getResultForVariation("senior").getFieldValue("$Value$Premium"), new DoubleValue(1290));
        assertEquals(resultsPolicies.getResultForVariation(NoVariation.ORIGIANAL_CALCULATION).getFieldValue("$Value$Premium"),
            new DoubleValue(1090));
    }

    @Test
    public void testArgumentReplacementVariation() throws Exception {
        VariationsEnhancer variationsEnhancer = new VariationsEnhancer(instantiationStrategy);
        variationsEnhancer.setServiceClass(EnhancedInterface.class);
        EnhancedInterface instance = (EnhancedInterface) variationsEnhancer.instantiate();
        Driver[] drivers = instance.getDriverProfiles1();
        VariationsResult<String> variationsResult = instance.driverAgeType(drivers[0],
            new VariationsPack(new ArgumentReplacementVariation("young", 0, drivers[1])));
        assertTrue(variationsResult.getVariationFailures().isEmpty());
        assertEquals(variationsResult.getResultForVariation("young"), YOUNG);
        assertEquals(variationsResult.getResultForVariation(NoVariation.ORIGIANAL_CALCULATION), STANDART);
    }

    public static interface EnhancedInterface {
        String driverAgeType(Driver driver);

        VariationsResult<String> driverAgeType(Driver driver, VariationsPack variations);

        SpreadsheetResult processPolicy(Policy policy);

        VariationsResult<SpreadsheetResult> processPolicy(Policy policy, VariationsPack variations);

        Driver[] getDriverProfiles1();

        Policy[] getPolicyProfile1();
    }

    // without original methods.
    public static interface WrongEnhancedInterface {
        VariationsResult<String> driverAgeType(Driver driver, VariationsPack variations);

        VariationsResult<SpreadsheetResult> processPolicy(Policy policy, VariationsPack variations);

        Driver[] getDriverProfiles1();

        Policy[] getPolicyProfile1();
    }
}
