package org.openl.rules.model.scaffolding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpreadsheetResultModel implements Model {

    private String name;
    private List<InputParameter> parameters;
    private String type;
    private List<StepModel> steps = new ArrayList<>();

    public SpreadsheetResultModel() {
    }

    public SpreadsheetResultModel(String name,
            List<InputParameter> parameterModels,
            String type,
            List<StepModel> steps) {
        this.name = name;
        this.parameters = parameterModels;
        this.type = type;
        this.steps = steps;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<InputParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<InputParameter> parameters) {
        this.parameters = parameters;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<StepModel> getSteps() {
        return steps;
    }

    public void setSteps(List<StepModel> steps) {
        this.steps = steps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        SpreadsheetResultModel that = (SpreadsheetResultModel) o;

        if (!Objects.equals(name, that.name))
            return false;
        if (!Objects.equals(parameters, that.parameters))
            return false;
        if (!Objects.equals(type, that.type))
            return false;
        return Objects.equals(steps, that.steps);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (steps != null ? steps.hashCode() : 0);
        return result;
    }
}
