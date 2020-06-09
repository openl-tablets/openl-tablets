package org.openl.rules.project.instantiation;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.openl.dependency.IDependencyManager;
import org.openl.generated.test.beans.Driver;
import org.openl.generated.test.beans.Policy;
import org.openl.meta.DoubleValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.project.instantiation.variation.VariationInstantiationStrategyEnhancer;
import org.openl.rules.project.instantiation.variation.VariationInstantiationStrategyEnhancerHelper;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.variation.*;

public class VariationsTest {
    private static final String TEST_PROJECT_FOLDER = "test-resources/dependencies/test4/module/dependency-module1";
    private ProjectResolver projectResolver = ProjectResolver.getInstance();
    private ApiBasedInstantiationStrategy instantiationStrategy;

    @Before
    public void init() throws Exception {
        File tut4Folder = new File(TEST_PROJECT_FOLDER);
        ProjectDescriptor project = projectResolver.resolve(tut4Folder);
        IDependencyManager dependencyManager = new SimpleDependencyManager(Collections
            .singletonList(project), null, true, true, null);
        instantiationStrategy = new ApiBasedInstantiationStrategy(project.getModules().get(0), dependencyManager, true);
    }

    @Test
    public void testEnhancement() throws Exception {
        // without interface
        VariationInstantiationStrategyEnhancer variationsEnhancer = new VariationInstantiationStrategyEnhancer(
            instantiationStrategy);
        assertTrue(
            VariationInstantiationStrategyEnhancerHelper.isDecoratedClass(variationsEnhancer.getInstanceClass()));
        assertFalse(
            VariationInstantiationStrategyEnhancerHelper.isDecoratedClass(instantiationStrategy.getInstanceClass()));

        // with correct interface
        VariationInstantiationStrategyEnhancer variationsEnhancerWithInterface = new VariationInstantiationStrategyEnhancer(
            instantiationStrategy);
        variationsEnhancerWithInterface.setServiceClass(EnhancedInterface.class);
        assertTrue(VariationInstantiationStrategyEnhancerHelper
            .isDecoratedClass(variationsEnhancerWithInterface.getInstanceClass()));
        assertFalse(
            VariationInstantiationStrategyEnhancerHelper.isDecoratedClass(instantiationStrategy.getInstanceClass()));

        // with wrong interface
        // we use simple api instantiation strategy because wrapper
        // instantiation strategy always has service class that cannot be
        // modified
        File folder = new File(new File(TEST_PROJECT_FOLDER, "rules"), "main");
        ProjectDescriptor project = projectResolver.resolve(folder);
        IDependencyManager dependencyManager = new SimpleDependencyManager(Collections
            .singletonList(project), null, true, true, null);
        ApiBasedInstantiationStrategy instantiationStrategy = new ApiBasedInstantiationStrategy(project.getModules()
            .get(0), dependencyManager, true);
        VariationInstantiationStrategyEnhancer variationsEnhancerWithWrongInterface = new VariationInstantiationStrategyEnhancer(
            instantiationStrategy);
        variationsEnhancerWithWrongInterface.setServiceClass(WrongEnhancedInterface.class);
        try {
            variationsEnhancerWithWrongInterface.instantiate();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Failed to find corresrponding method in original class for method"));
        }
    }

    @Test
    public void testVariationsFactory() {
        Variation argumentReplacementVariation = VariationsFactory.getVariation("changeArg", 2, ".", new Object());
        assertTrue(argumentReplacementVariation instanceof ArgumentReplacementVariation);
        assertEquals(((ArgumentReplacementVariation) argumentReplacementVariation).getUpdatedArgumentIndex(), 2);
        String path = "drivers[name = 'Sara']/age";
        Variation jxpathVariation = VariationsFactory.getVariation("jxpath", 3, path, new Object());
        assertTrue(jxpathVariation instanceof JXPathVariation);
        assertEquals(((JXPathVariation) jxpathVariation).getPath(), path);
        Variation cloningVariation = VariationsFactory.getVariation("clone", 1, path, new Object(), true);
        assertTrue(cloningVariation instanceof DeepCloningVariation);
        assertTrue(((DeepCloningVariation) cloningVariation).getDelegatedVariation() instanceof JXPathVariation);
    }

    @Test
    public void testJXPathVariation() throws Exception {
        VariationInstantiationStrategyEnhancer variationsEnhancer = new VariationInstantiationStrategyEnhancer(
            instantiationStrategy);
        variationsEnhancer.setServiceClass(EnhancedInterface.class);
        EnhancedInterface instance = (EnhancedInterface) variationsEnhancer.instantiate();
        Driver[] drivers = instance.getDriverProfiles1();
        VariationsResult<String> resultsDrivers = instance.driverAgeType(drivers[0],
            new VariationsPack(new JXPathVariation("young", 0, "age", 18),
                new JXPathVariation("senior", 0, "age", 71)));
        assertTrue(resultsDrivers.getVariationFailures().isEmpty());
        assertEquals(resultsDrivers.getResultForVariation("young"), "Young Driver");
        assertEquals(resultsDrivers.getResultForVariation("senior"), "Senior Driver");
        assertEquals(resultsDrivers.getResultForVariation(NoVariation.ORIGINAL_CALCULATION), "Standard Driver");
        Policy[] policies = instance.getPolicyProfile1();
        VariationsResult<SpreadsheetResult> resultsPolicies = instance.processPolicy(policies[0],
            new VariationsPack(new JXPathVariation("young", 0, "drivers[name = 'Sara']/age", 17),
                new JXPathVariation("senior", 0, "drivers[name = 'Sara']/age", 88)));
        assertEquals(3, resultsPolicies.getAllProcessedVariationIDs().length);
        assertTrue(resultsPolicies.getVariationFailures().isEmpty());
        assertEquals(resultsPolicies.getResultForVariation("young").getFieldValue("$Value$Premium"),
            new DoubleValue(1390));
        assertEquals(resultsPolicies.getResultForVariation("senior").getFieldValue("$Value$Premium"),
            new DoubleValue(1290));
        assertEquals(
            resultsPolicies.getResultForVariation(NoVariation.ORIGINAL_CALCULATION).getFieldValue("$Value$Premium"),
            new DoubleValue(1090));
    }

    @Test
    public void testArgumentReplacementVariation() throws Exception {
        VariationInstantiationStrategyEnhancer variationsEnhancer = new VariationInstantiationStrategyEnhancer(
            instantiationStrategy);
        variationsEnhancer.setServiceClass(EnhancedInterface.class);
        EnhancedInterface instance = (EnhancedInterface) variationsEnhancer.instantiate();
        Driver[] drivers = instance.getDriverProfiles1();
        VariationsResult<String> variationsResult = instance.driverAgeType(drivers[0],
            new VariationsPack(new ArgumentReplacementVariation("young", 0, drivers[1])));
        assertTrue(variationsResult.getVariationFailures().isEmpty());
        assertEquals(variationsResult.getResultForVariation("young"), "Young Driver");
        assertEquals(variationsResult.getResultForVariation(NoVariation.ORIGINAL_CALCULATION), "Standard Driver");
    }

    public interface EnhancedInterface {
        String driverAgeType(Driver driver);

        VariationsResult<String> driverAgeType(Driver driver, VariationsPack variations);

        SpreadsheetResult processPolicy(Policy policy);

        VariationsResult<SpreadsheetResult> processPolicy(Policy policy, VariationsPack variations);

        Driver[] getDriverProfiles1();

        Policy[] getPolicyProfile1();
    }

    // without original methods.
    public interface WrongEnhancedInterface {
        VariationsResult<String> driverAgeType(Driver driver, VariationsPack variations);

        VariationsResult<SpreadsheetResult> processPolicy(Policy policy, VariationsPack variations);

        Driver[] getDriverProfiles1();

        Policy[] getPolicyProfile1();
    }
}
