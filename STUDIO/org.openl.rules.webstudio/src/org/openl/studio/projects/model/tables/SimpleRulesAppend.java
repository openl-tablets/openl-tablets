package org.openl.studio.projects.model.tables;

import java.util.LinkedHashMap;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request model for append lines to {@code SimpleRules} table
 *
 * @author Vladyslav Pikus
 */
public class SimpleRulesAppend implements AppendTableView {

    @Schema(description = "List of rule rows to append as key-value maps")
    private List<LinkedHashMap<String, Object>> rules;

    public List<LinkedHashMap<String, Object>> getRules() {
        return rules;
    }

    public void setRules(List<LinkedHashMap<String, Object>> rules) {
        this.rules = rules;
    }

    @Override
    public String getTableType() {
        return SimpleRulesView.TABLE_TYPE;
    }

    public void setTableType(String tableType) {
        // no op
    }
}
