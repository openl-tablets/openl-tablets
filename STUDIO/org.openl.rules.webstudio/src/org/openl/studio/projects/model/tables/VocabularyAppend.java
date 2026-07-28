package org.openl.studio.projects.model.tables;

import java.util.Collection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Request model for append lines to {@code Vocabulary} table
 *
 * @author Vladyslav Pikus
 */
public class VocabularyAppend implements AppendTableView {

    @Getter
    @Schema(description = "Collection of vocabulary values to append")
    @Setter
    private Collection<VocabularyValueView> values;

    @Override
    public String getTableType() {
        return VocabularyView.TABLE_TYPE;
    }

    public void setTableType(String tableType) {
        // no op
    }
}
