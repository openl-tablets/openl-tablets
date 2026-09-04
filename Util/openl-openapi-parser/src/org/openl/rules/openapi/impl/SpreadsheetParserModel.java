package org.openl.rules.openapi.impl;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.model.scaffolding.SpreadsheetModel;

public class SpreadsheetParserModel {
    @Getter
    @Setter
    private SpreadsheetModel model;
    @Getter
    @Setter
    private String returnRef;
    @Getter
    private boolean refIsDataType;

    public void setStoreInModels(boolean hasSelfLink) {
        this.refIsDataType = hasSelfLink;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SpreadsheetParserModel that)) {
            return false;
        }

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
