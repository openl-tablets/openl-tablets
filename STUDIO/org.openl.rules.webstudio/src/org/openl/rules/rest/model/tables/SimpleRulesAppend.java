package org.openl.rules.rest.model.tables;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Request model for append lines to {@code SimpleRules} table
 *
 * @author Vladyslav Pikus
 */
public class SimpleRulesAppend implements AppendTableView {

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
}
