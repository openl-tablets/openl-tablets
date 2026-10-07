package org.openl.studio.projects.model.tables;

import java.util.LinkedHashMap;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Request model for append lines to {@code SmartRules} table
 *
 * @author Vladyslav Pikus
 */
public class SmartRulesAppend implements AppendTableView {

    @Getter
    @Schema(description = "List of smart rule rows to append as key-value maps with hierarchical structure")
    @Setter
    private List<LinkedHashMap<String, Object>> rules;

    @Override
    public String getTableType() {
        return SmartRulesView.TABLE_TYPE;
    }

    public void setTableType(String tableType) {
        // no op
    }
}
