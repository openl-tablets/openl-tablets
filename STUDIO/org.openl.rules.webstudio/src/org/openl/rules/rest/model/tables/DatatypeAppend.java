package org.openl.rules.rest.model.tables;

import java.util.Collection;

/**
 * Request model for append lines to datatype table
 *
 * @author Vladyslav Pikus
 */
public class DatatypeAppend implements AppendTableView {

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
}
