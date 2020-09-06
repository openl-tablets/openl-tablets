package org.open.rules.model.scaffolding;

import org.junit.Test;
import org.openl.rules.model.scaffolding.environment.EnvironmentModel;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EnvironmentModelTest {

    @Test
    public void testEnvironmentModel() {
        EnvironmentModel envWithImports = new EnvironmentModel();
        List<String> imports = Arrays.asList("Model", "CarModel", "MyModel");
        envWithImports.setImports(imports);
        assertEquals(3, envWithImports.getImports().size());
        assertEquals("Environment", envWithImports.getName());

        EnvironmentModel envWithDependencies = new EnvironmentModel();
        List<String> dependencies = Arrays.asList("dependencyExample", "CarDependency", "MyDependency");
        envWithDependencies.setDependencies(dependencies);
        assertEquals(3, envWithDependencies.getDependencies().size());
        assertEquals("Environment", envWithDependencies.getName());

        EnvironmentModel env = new EnvironmentModel(imports, dependencies);
        assertEquals(3, env.getImports().size());
        assertEquals(3, env.getDependencies().size());
    }
}
