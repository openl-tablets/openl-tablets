package org.openl.rules.openapi.impl;

import org.openl.rules.model.scaffolding.SpreadsheetModel;

import java.util.Objects;

public class SpreadsheetParserModel {
    private SpreadsheetModel model;
    private String returnRef;
    private boolean refIsDataType;

    public SpreadsheetParserModel() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SpreadsheetParserModel that = (SpreadsheetParserModel) o;

        if (refIsDataType != that.refIsDataType) {
            return false;
        }
        if (!Objects.equals(model, that.model)) {
            return false;
        }
        return Objects.equals(returnRef, that.returnRef);
    }

    @Override
    public int hashCode() {
        int result = model != null ? model.hashCode() : 0;
        result = 31 * result + (returnRef != null ? returnRef.hashCode() : 0);
        result = 31 * result + (refIsDataType ? 1 : 0);
        return result;
    }
}
