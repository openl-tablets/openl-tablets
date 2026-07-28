package org.openl.studio.projects.model.tables;

import java.util.Collection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Request model for append lines to datatype table
 *
 * @author Vladyslav Pikus
 */
public class DatatypeAppend implements AppendTableView {

    @Getter
    @Schema(description = "Collection of fields to append to the datatype")
    @Setter
    private Collection<DatatypeFieldView> fields;

    @Override
    public String getTableType() {
        return DatatypeView.TABLE_TYPE;
    }

    public void setTableType(String tableType) {
        // no op
    }
}
