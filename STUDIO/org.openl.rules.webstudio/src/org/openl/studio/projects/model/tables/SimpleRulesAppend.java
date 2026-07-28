package org.openl.studio.projects.model.tables;

import java.util.LinkedHashMap;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Request model for append lines to {@code SimpleRules} table
 *
 * @author Vladyslav Pikus
 */
public class SimpleRulesAppend implements AppendTableView {

    @Getter
    @Schema(description = "List of rule rows to append as key-value maps")
    @Setter
    private List<LinkedHashMap<String, Object>> rules;

    @Override
    public String getTableType() {
        return SimpleRulesView.TABLE_TYPE;
    }

    public void setTableType(String tableType) {
        // no op
    }
}
