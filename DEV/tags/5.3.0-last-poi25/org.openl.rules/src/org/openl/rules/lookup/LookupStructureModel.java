package org.openl.rules.lookup;

import org.openl.types.IMethodSignature;

public class LookupStructureModel {
    String header;
    IMethodSignature signature;
    LookupKeyStructure[] rowsOrColumns;

    public String getHeader() {
        return header;
    }

    public LookupKeyStructure[] getRowsOrColumns() {
        return rowsOrColumns;
    }

    public IMethodSignature getSignature() {
        return signature;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void setRowsOrColumns(LookupKeyStructure[] rowsOrColumns) {
        this.rowsOrColumns = rowsOrColumns;
    }

    public void setSignature(IMethodSignature signature) {
        this.signature = signature;
    }
}
