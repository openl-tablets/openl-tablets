package org.openl.rules.model.scaffolding;

import java.util.ArrayList;
import java.util.List;

public class SpreadsheetResultModel {

    private String signature;
    private String type;
    private List<StepModel> steps = new ArrayList<>();

    public SpreadsheetResultModel(String signature, String type, List<StepModel> steps) {
        this.signature = signature;
        this.type = type;
        this.steps = steps;
    }

    public SpreadsheetResultModel() {

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

    public List<StepModel> getSteps() {
        return steps;
    }

    public void setSteps(List<StepModel> steps) {
        this.steps = steps;
    }
}
