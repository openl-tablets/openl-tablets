package org.openl.extension.xmlrules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.openl.extension.xmlrules.model.Field;
import org.openl.extension.xmlrules.model.Function;
import org.openl.extension.xmlrules.model.Table;
import org.openl.extension.xmlrules.model.Type;
import org.openl.extension.xmlrules.model.single.node.RangeNode;

// TODO Check, that this data is cleared when it's not needed anymore
public class ProjectData {
    private static final ThreadLocal<ProjectData> INSTANCE = new ThreadLocal<ProjectData>();
    private static final ThreadLocal<Unmarshaller> unmarshallerThreadLocal = new ThreadLocal<Unmarshaller>();

    public static ProjectData getCurrentInstance() {
        ProjectData projectData = INSTANCE.get();

        if (projectData == null) {
            projectData = new ProjectData();
            INSTANCE.set(projectData);
        }

        return projectData;
    }

    public static void setCurrentInstance(ProjectData projectData) {
        INSTANCE.set(projectData);
    }

    public static void removeCurrentInstance() {
        INSTANCE.remove();
    }

    private final Set<Type> types = new HashSet<Type>();
    private final Set<Function> functions = new HashSet<Function>();
    private final Set<Table> tables = new HashSet<Table>();

    private final Set<String> typeNames = new HashSet<String>();
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
        types.add(type);

        typeNames.add(type.getName());
        for (Field field : type.getFields()) {
            fieldNames.add(field.getName());
        }
    }

    public void addFunction(Function function) {
        functions.add(function);
    }

    public void addTable(Table table) {
        tables.add(table);
    }

    public void addNamedRange(String name, RangeNode rangeNode) {
        namedRanges.put(name, rangeNode);
    }

    public Set<Type> getTypes() {
        return types;
    }

    public Set<String> getTypeNames() {
        return typeNames;
    }

    public Set<String> getFieldNames() {
        return fieldNames;
    }

    public Map<String, RangeNode> getNamedRanges() {
        return namedRanges;
    }

    public Set<Function> getFunctions() {
        return functions;
    }

    public Set<Table> getTables() {
        return tables;
    }
}
