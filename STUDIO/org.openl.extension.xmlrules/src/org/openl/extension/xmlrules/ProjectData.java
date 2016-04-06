package org.openl.extension.xmlrules;

import java.util.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.openl.extension.xmlrules.model.Field;
import org.openl.extension.xmlrules.model.Function;
import org.openl.extension.xmlrules.model.Table;
import org.openl.extension.xmlrules.model.Type;
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
    private final Map<String, Function> functions = new HashMap<String, Function>();
    private final Map<String, Table> tables = new HashMap<String, Table>();

    private final Set<String> fieldNames = new HashSet<String>();

    private final Map<String, RangeNode> namedRanges = new HashMap<String, RangeNode>();

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
        functions.put(function.getName().toLowerCase(), function);
    }

    public void addTable(Table table) {
        tables.put(table.getName().toLowerCase(), table);
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

    public Function getFunction(String functionName) {
        return functionName == null ? null : functions.get(functionName.toLowerCase());
    }

    public Table getTable(String tableName) {
        return tableName == null ? null : tables.get(tableName.toLowerCase());
    }

}
