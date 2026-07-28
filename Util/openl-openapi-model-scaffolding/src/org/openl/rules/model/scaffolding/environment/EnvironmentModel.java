package org.openl.rules.model.scaffolding.environment;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.model.scaffolding.Model;

public class EnvironmentModel implements Model {
    public static final String ENVIRONMENT = "Environment";
    @Getter
    @Setter
    private List<String> imports = new ArrayList<>();
    @Getter
    @Setter
    private List<String> dependencies = new ArrayList<>();

    public EnvironmentModel() {
    }

    public EnvironmentModel(List<String> imports, List<String> dependencies) {
        this.imports = imports;
        this.dependencies = dependencies;
    }

    @Override
    public String getName() {
        return ENVIRONMENT;
    }
}
