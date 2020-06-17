package org.openl.rules.model.scaffolding;

import java.util.ArrayList;
import java.util.List;

public class ProjectModel {
    private String name;
    private List<DatatypeModel> datatypeModels = new ArrayList<>();
    private List<SpreadsheetResultModel> spreadsheetResultModels = new ArrayList<>();

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
}
