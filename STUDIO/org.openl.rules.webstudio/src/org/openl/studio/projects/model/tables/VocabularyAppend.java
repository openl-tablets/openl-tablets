package org.openl.studio.projects.model.tables;

import java.util.Collection;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request model for append lines to {@code Vocabulary} table
 *
 * @author Vladyslav Pikus
 */
public class VocabularyAppend implements AppendTableView {

    @Schema(description = "Collection of vocabulary values to append")
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

    public void setTableType(String tableType) {
        // no op
    }
}
