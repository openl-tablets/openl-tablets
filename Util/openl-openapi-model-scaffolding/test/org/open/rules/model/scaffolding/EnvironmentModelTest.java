package org.open.rules.model.scaffolding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.openl.rules.model.scaffolding.environment.EnvironmentModel;

class EnvironmentModelTest {

    @Test
    void testEnvironmentModel() {
        var envWithImports = new EnvironmentModel();
        var imports = Arrays.asList("Model", "CarModel", "MyModel");
        envWithImports.setImports(imports);
        assertEquals(3, envWithImports.getImports().size());
        assertEquals("Environment", envWithImports.getName());

        var envWithDependencies = new EnvironmentModel();
        var dependencies = Arrays.asList("dependencyExample", "CarDependency", "MyDependency");
        envWithDependencies.setDependencies(dependencies);
        assertEquals(3, envWithDependencies.getDependencies().size());
        assertEquals("Environment", envWithDependencies.getName());

        var env = new EnvironmentModel(imports, dependencies);
        assertEquals(3, env.getImports().size());
        assertEquals(3, env.getDependencies().size());
    }
}
