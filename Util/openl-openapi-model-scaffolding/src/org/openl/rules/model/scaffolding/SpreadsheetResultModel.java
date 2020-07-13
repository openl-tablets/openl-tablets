package org.openl.rules.model.scaffolding;

import java.util.ArrayList;
import java.util.List;

public class SpreadsheetResultModel implements Model {

    private String name;
    private String signature;
    private String type;
    private List<FieldModel> steps = new ArrayList<>();

    public SpreadsheetResultModel(String name, String signature, String type, List<FieldModel> steps) {
        this.name = name;
        this.signature = signature;
        this.type = type;
        this.steps = steps;
    }

    public SpreadsheetResultModel() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<FieldModel> getSteps() {
        return steps;
    }

    public void setSteps(List<FieldModel> steps) {
        this.steps = steps;
    }
}
