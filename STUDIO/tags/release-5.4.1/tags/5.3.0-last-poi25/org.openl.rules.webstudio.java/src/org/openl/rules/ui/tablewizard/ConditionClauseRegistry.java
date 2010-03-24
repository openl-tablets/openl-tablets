package org.openl.rules.ui.tablewizard;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import javax.faces.model.SelectItem;

/**
 * @author Aliaksandr Antonik.
 */
public class ConditionClauseRegistry {
    private static ConditionClauseRegistry instance = new ConditionClauseRegistry();
    private static final Set<String> numericClasses;
    private Map<String, List<ConditionClauseVariant>> registry;

    private Map<String, SelectItem[]> selectItems;
    private Map<Long, ConditionClauseVariant> id2Variant = new HashMap<Long, ConditionClauseVariant>();
    static {
        numericClasses = new HashSet<String>();
        numericClasses.add("int");
        numericClasses.add("byte");
        numericClasses.add("short");
        numericClasses.add("long");
        numericClasses.add("float");
        numericClasses.add("double");
        numericClasses.add("char");
    }

    public static ConditionClauseRegistry getInstance() {
        return instance;
    }

    private static String getType(String clazz) {
        if (numericClasses.contains(clazz)) {
            return "numeric";
        }
        if (String.class.getName().equals(clazz)) {
            return "string";
        }
        if ("boolean".equals(clazz)) {
            return "boolean";
        }
        return "object";
    }

    public ConditionClauseRegistry() {
        registry = new HashMap<String, List<ConditionClauseVariant>>();

        List<ConditionClauseVariant> numericClauses = new ArrayList<ConditionClauseVariant>();
        numericClauses.add(new ConditionClauseVariant("less", "{0} < {1}"));
        numericClauses.add(new ConditionClauseVariant("more", "{0} > {1}"));
        numericClauses.add(new ConditionClauseVariant("equals", "{0} == {1}"));
        registry.put("numeric", numericClauses);

        List<ConditionClauseVariant> stringClauses = new ArrayList<ConditionClauseVariant>();
        stringClauses.add(new ConditionClauseVariant("starts", "{0}.startsWith({1})"));
        stringClauses.add(new ConditionClauseVariant("ends", "{0}.endsWith({1})"));
        stringClauses.add(new ConditionClauseVariant("equals", "{0}.equals({1})"));
        registry.put("string", stringClauses);

        List<ConditionClauseVariant> dateClauses = new ArrayList<ConditionClauseVariant>();
        dateClauses.add(new ConditionClauseVariant("equals", "{0}.equals({1})"));
        registry.put("object", dateClauses);

        List<ConditionClauseVariant> booleanClauses = new ArrayList<ConditionClauseVariant>();
        booleanClauses.add(new ConditionClauseVariant("equals", "{0} == {1})"));
        booleanClauses.add(new ConditionClauseVariant("not equal", "{0} != {1})"));
        registry.put("boolean", booleanClauses);

        index();
    }

    public ConditionClauseVariant get(long id) {
        return id2Variant.get(id);
    }

    public List<ConditionClauseVariant> getConditionClauseVariants(String clazz) {
        return registry.get(getType(clazz));
    }

    public SelectItem[] getItemsByType(String clazz) {
        return selectItems.get(getType(clazz));
    }

    private void index() {
        for (List<ConditionClauseVariant> conditions : registry.values()) {
            for (ConditionClauseVariant variant : conditions) {
                id2Variant.put(variant.getId(), variant);
            }
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
}
