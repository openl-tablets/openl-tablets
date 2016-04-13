package org.openl.extension.xmlrules;

import java.util.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.openl.extension.xmlrules.model.Field;
import org.openl.extension.xmlrules.model.Function;
import org.openl.extension.xmlrules.model.Table;
import org.openl.extension.xmlrules.model.Type;
import org.openl.extension.xmlrules.model.lazy.LazyAttributes;
import org.openl.extension.xmlrules.model.single.node.RangeNode;

public class ProjectData {
    private static final ThreadLocal<ProjectData> INSTANCE = new ThreadLocal<ProjectData>();
    private static final ThreadLocal<Unmarshaller> unmarshallerThreadLocal = new ThreadLocal<Unmarshaller>();

    public static ProjectData getCurrentInstance() {
        return INSTANCE.get();
    }

    public static void setCurrentInstance(ProjectData projectData) {
        INSTANCE.set(projectData);
    }

    public static void removeCurrentInstance() {
        INSTANCE.remove();
    }

    private final Map<String, Type> types = new HashMap<String, Type>();
    private final Map<String, List<Function>> functions = new HashMap<String, List<Function>>();
    private final Map<String, List<Table>> tables = new HashMap<String, List<Table>>();

    private final Set<String> fieldNames = new HashSet<String>();

    private final Map<String, RangeNode> namedRanges = new HashMap<String, RangeNode>();

    private LazyAttributes attributes;

    public static Unmarshaller getUnmarshaller() throws JAXBException {
        Unmarshaller unmarshaller = unmarshallerThreadLocal.get();
        if (unmarshaller == null) {
            JAXBContext context = JAXBContext.newInstance("org.openl.extension.xmlrules.model.single");
            unmarshaller = context.createUnmarshaller();
            unmarshallerThreadLocal.set(unmarshaller);
        }
        return unmarshaller;
    }

    public static void clearUnmarshaller() {
        unmarshallerThreadLocal.remove();
    }

    public void addType(Type type) {
        types.put(type.getName().toLowerCase(), type);

        for (Field field : type.getFields()) {
            fieldNames.add(field.getName().toLowerCase());
        }
    }

    public void addFunction(Function function) {
        String key = function.getName().toLowerCase();
        List<Function> overloadedFunctions = functions.get(key);
        if (overloadedFunctions == null) {
            overloadedFunctions = new ArrayList<Function>();
            functions.put(key, overloadedFunctions);
        }
        overloadedFunctions.add(function);
    }

    public void addTable(Table table) {
        String key = table.getName().toLowerCase();
        List<Table> overloadedTables = tables.get(key);
        if (overloadedTables == null) {
            overloadedTables = new ArrayList<Table>();
            tables.put(key, overloadedTables);
        }
        overloadedTables.add(table);
    }

    public void addNamedRange(String name, RangeNode rangeNode) {
        namedRanges.put(name, rangeNode);
    }

    public Type getType(String typeName) {
        return typeName == null ? null : types.get(typeName.toLowerCase());

    }

    public boolean containsType(String typeName) {
        return typeName != null && types.containsKey(typeName.toLowerCase());
    }

    public Collection<Type> getTypes() {
        return types.values();
    }

    public boolean containsField(String fieldName) {
        return fieldName != null && fieldNames.contains(fieldName.toLowerCase());
    }

    public Map<String, RangeNode> getNamedRanges() {
        return namedRanges;
    }

    public Function getFirstFunction(String functionName) {
        List<Function> overloadedFunctions = getOverloadedFunctions(functionName);
        return overloadedFunctions.isEmpty() ? null : overloadedFunctions.get(0);
    }

    public List<Function> getOverloadedFunctions(String functionName) {
        if (functionName == null) {
            return Collections.emptyList();
        }
        else {
            List<Function> overloadedFunctions = functions.get(functionName.toLowerCase());
            return overloadedFunctions == null ? Collections.<Function>emptyList() : overloadedFunctions;
        }
    }

    public Table getFirstTable(String tableName) {
        List<Table> overloadedTables = getOverloadedTables(tableName);
        return overloadedTables.isEmpty() ? null : overloadedTables.get(0);
    }

    public List<Table> getOverloadedTables(String tableName) {
        if (tableName == null) {
            return Collections.emptyList();
        }
        else {
            List<Table> overloadedTables = tables.get(tableName.toLowerCase());
            return overloadedTables == null ? Collections.<Table>emptyList() : overloadedTables;
        }
    }

    public LazyAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(LazyAttributes attributes) {
        this.attributes = attributes;
    }
}
