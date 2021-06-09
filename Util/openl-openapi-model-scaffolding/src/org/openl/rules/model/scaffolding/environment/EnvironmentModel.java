package org.openl.rules.model.scaffolding.environment;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.model.scaffolding.Model;

public class EnvironmentModel implements Model {
    public static final String ENVIRONMENT = "Environment";
    private List<String> imports = new ArrayList<>();
    private List<String> dependencies = new ArrayList<>();

    public EnvironmentModel() {
    }

    public EnvironmentModel(List<String> imports, List<String> dependencies) {
        this.imports = imports;
        this.dependencies = dependencies;
    }

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public String getName() {
        return ENVIRONMENT;
    }
}
