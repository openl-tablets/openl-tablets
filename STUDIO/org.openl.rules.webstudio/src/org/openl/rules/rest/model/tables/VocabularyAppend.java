package org.openl.rules.rest.model.tables;

import java.util.Collection;

/**
 * Request model for append lines to {@code Vocabulary} table
 *
 * @author Vladyslav Pikus
 */
public class VocabularyAppend implements AppendTableView {

    private Collection<VocabularyValueView> values;

    public Collection<VocabularyValueView> getValues() {
        return values;
    }

    public void setValues(Collection<VocabularyValueView> values) {
        this.values = values;
    }

    @Override
    public String getTableType() {
        return VocabularyView.TABLE_TYPE;
    }
}
