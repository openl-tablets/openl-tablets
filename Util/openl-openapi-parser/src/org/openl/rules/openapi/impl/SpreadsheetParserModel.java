package org.openl.rules.openapi.impl;

import org.openl.rules.model.scaffolding.SpreadsheetModel;

public class SpreadsheetParserModel {
    private SpreadsheetModel model;
    private String returnRef;
    private boolean refIsDataType;

    public SpreadsheetParserModel() {
    }

    public SpreadsheetParserModel(SpreadsheetModel model, String returnRef, boolean hasSelfLink) {
        this.model = model;
        this.returnRef = returnRef;
        this.refIsDataType = hasSelfLink;
    }

    public SpreadsheetModel getModel() {
        return model;
    }

    public void setModel(SpreadsheetModel model) {
        this.model = model;
    }

    public String getReturnRef() {
        return returnRef;
    }

    public void setReturnRef(String returnRef) {
        this.returnRef = returnRef;
    }

    public boolean isRefIsDataType() {
        return refIsDataType;
    }

    public void setStoreInModels(boolean hasSelfLink) {
        this.refIsDataType = hasSelfLink;
    }
}
