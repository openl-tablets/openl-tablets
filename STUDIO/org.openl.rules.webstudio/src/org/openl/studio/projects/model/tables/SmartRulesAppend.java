package org.openl.studio.projects.model.tables;

import java.util.LinkedHashMap;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request model for append lines to {@code SmartRules} table
 *
 * @author Vladyslav Pikus
 */
public class SmartRulesAppend implements AppendTableView {

    @Schema(description = "List of smart rule rows to append as key-value maps with hierarchical structure")
    private List<LinkedHashMap<String, Object>> rules;

    public List<LinkedHashMap<String, Object>> getRules() {
        return rules;
    }

    public void setRules(List<LinkedHashMap<String, Object>> rules) {
        this.rules = rules;
    }

    @Override
    public String getTableType() {
        return SmartRulesView.TABLE_TYPE;
    }

    public void setTableType(String tableType) {
        // no op
    }
}
