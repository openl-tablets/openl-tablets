package org.openl.rules.model.scaffolding;

public class SpreadsheetResultModel {

    private String signature;
    private String type;
    private DatatypeModel model;

    public SpreadsheetResultModel(String signature, String type, DatatypeModel model) {
        this.signature = signature;
        this.type = type;
        this.model = model;
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

    public DatatypeModel getModel() {
        return model;
    }

    public void setModel(DatatypeModel model) {
        this.model = model;
    }
}
