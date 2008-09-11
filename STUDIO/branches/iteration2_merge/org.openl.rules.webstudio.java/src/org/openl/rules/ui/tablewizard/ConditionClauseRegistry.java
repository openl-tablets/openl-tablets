package org.openl.rules.ui.tablewizard;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import javax.faces.model.SelectItem;

/**
 * @author Aliaksandr Antonik.
 */
public class ConditionClauseRegistry {
    private Map<String, List<ConditionClauseVariant>> registry;
    private Map<String, SelectItem[]> selectItems;
    private Map<Long, ConditionClauseVariant> id2Variant = new HashMap<Long, ConditionClauseVariant>();

    private static ConditionClauseRegistry instance = new ConditionClauseRegistry();

    public ConditionClauseRegistry() {
        registry = new HashMap<String, List<ConditionClauseVariant>>();

        List<ConditionClauseVariant> intClauses = new ArrayList<ConditionClauseVariant>();
        intClauses.add(new ConditionClauseVariant("less", "{0} < {1}"));
        intClauses.add(new ConditionClauseVariant("more", "{0} > {1}"));
        intClauses.add(new ConditionClauseVariant("equals", "{0} == {1}"));
        registry.put("int", intClauses);

        List<ConditionClauseVariant> stringClauses = new ArrayList<ConditionClauseVariant>();
        stringClauses.add(new ConditionClauseVariant("starts", "{0}.startsWith({1})"));
        stringClauses.add(new ConditionClauseVariant("ends", "{0}.endsWith({1})"));
        stringClauses.add(new ConditionClauseVariant("equals", "{0}.equals({1})"));
        registry.put("String", stringClauses);

        List<ConditionClauseVariant> dateClauses = new ArrayList<ConditionClauseVariant>();
        dateClauses.add(new ConditionClauseVariant("equals", "{0}.equals({1})"));
        registry.put("Date", dateClauses);

        index();
    }

    private void index() {
        for (List<ConditionClauseVariant> conditions : registry.values())
            for (ConditionClauseVariant variant : conditions) {
                id2Variant.put(variant.getId(), variant);
            }

        selectItems = new HashMap<String, SelectItem[]>();
        for (Map.Entry<String, List<ConditionClauseVariant>> entry : registry.entrySet()) {
            SelectItem[] items = new SelectItem[entry.getValue().size()];
            int pos = 0;
            for (ConditionClauseVariant v : entry.getValue()) {
                items[pos++] = new SelectItem(v.getId(), v.getDisplayValue());
            }
            selectItems.put(entry.getKey(), items);
        }
    }

    public SelectItem[] getItemsByType(String type) {
        return selectItems.get(type);
    }

    public List<ConditionClauseVariant> getConditionClauseVariants(String type) {
        return registry.get(type);
    }

    public ConditionClauseVariant get(long id) {
        return id2Variant.get(id);
    }

    public static ConditionClauseRegistry getInstance() {
        return instance;
    }
}
