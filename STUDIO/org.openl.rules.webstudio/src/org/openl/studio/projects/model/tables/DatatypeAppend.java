package org.openl.studio.projects.model.tables;

import java.util.Collection;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request model for append lines to datatype table
 *
 * @author Vladyslav Pikus
 */
public class DatatypeAppend implements AppendTableView {

    @Schema(description = "Collection of fields to append to the datatype")
    private Collection<DatatypeFieldView> fields;

    public Collection<DatatypeFieldView> getFields() {
        return fields;
    }

    public void setFields(Collection<DatatypeFieldView> fields) {
        this.fields = fields;
    }

    @Override
    public String getTableType() {
        return DatatypeView.TABLE_TYPE;
    }

    public void setTableType(String tableType) {
        // no op
    }
}
