package org.openl.rules.model.scaffolding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProjectModel {

    private String name;
    private List<DatatypeModel> datatypeModels = new ArrayList<>();
    private List<SpreadsheetResultModel> spreadsheetResultModels = new ArrayList<>();

    public ProjectModel() {
    }

    public ProjectModel(String name,
            List<DatatypeModel> datatypeModels,
            List<SpreadsheetResultModel> spreadsheetResultModels) {
        this.name = name;
        this.datatypeModels = datatypeModels;
        this.spreadsheetResultModels = spreadsheetResultModels;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DatatypeModel> getDatatypeModels() {
        return datatypeModels;
    }

    public List<SpreadsheetResultModel> getSpreadsheetResultModels() {
        return spreadsheetResultModels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ProjectModel that = (ProjectModel) o;

        if (!Objects.equals(name, that.name))
            return false;
        if (!Objects.equals(datatypeModels, that.datatypeModels))
            return false;
        return Objects.equals(spreadsheetResultModels, that.spreadsheetResultModels);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (datatypeModels != null ? datatypeModels.hashCode() : 0);
        result = 31 * result + (spreadsheetResultModels != null ? spreadsheetResultModels.hashCode() : 0);
        return result;
    }
}
