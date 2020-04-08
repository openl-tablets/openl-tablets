package org.openl.rules.ui.tablewizard;

import static org.openl.types.java.JavaOpenClass.BOOLEAN;
import static org.openl.types.java.JavaOpenClass.BYTE;
import static org.openl.types.java.JavaOpenClass.CHAR;
import static org.openl.types.java.JavaOpenClass.DOUBLE;
import static org.openl.types.java.JavaOpenClass.FLOAT;
import static org.openl.types.java.JavaOpenClass.INT;
import static org.openl.types.java.JavaOpenClass.LONG;
import static org.openl.types.java.JavaOpenClass.SHORT;
import static org.openl.types.java.JavaOpenClass.STRING;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.ClassUtils;
import org.openl.base.INamedThing;
import org.openl.meta.IMetaInfo;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetOpenClass;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ComponentTypeArrayOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * @author Aliaksandr Antonik.
 */
public class DomainTree {
    private static final Set<String> ignoredTypes;
    private static Map<String, IOpenClass> predefinedTypes;

    private Map<String, IOpenClass> treeElements;

    static {
        ignoredTypes = new HashSet<>();
        ignoredTypes.add("Object");
        ignoredTypes.add("Class");
        ignoredTypes.add("Void");
        ignoredTypes.add("void");
    }

    static {
        predefinedTypes = new LinkedHashMap<>();

        // The most popular
        predefinedTypes.put("String", STRING);
        predefinedTypes.put("Double", JavaOpenClass.getOpenClass(Double.class));
        predefinedTypes.put("Integer", JavaOpenClass.getOpenClass(Integer.class));
        predefinedTypes.put("Boolean", JavaOpenClass.getOpenClass(Boolean.class));
        predefinedTypes.put("Date", JavaOpenClass.getOpenClass(Date.class));

        predefinedTypes.put("BigInteger", JavaOpenClass.getOpenClass(BigInteger.class));
        predefinedTypes.put("BigDecimal", JavaOpenClass.getOpenClass(BigDecimal.class));

        predefinedTypes.put("IntRange", JavaOpenClass.getOpenClass(IntRange.class));
        predefinedTypes.put("DoubleRange", JavaOpenClass.getOpenClass(DoubleRange.class));

        predefinedTypes.put("Long", JavaOpenClass.getOpenClass(Long.class));
        predefinedTypes.put("Float", JavaOpenClass.getOpenClass(Float.class));
        predefinedTypes.put("Short", JavaOpenClass.getOpenClass(Short.class));
        predefinedTypes.put("Character", JavaOpenClass.getOpenClass(Character.class));

        // Less popular
        predefinedTypes.put("byte", BYTE);
        predefinedTypes.put("short", SHORT);
        predefinedTypes.put("int", INT);
        predefinedTypes.put("long", LONG);
        predefinedTypes.put("float", FLOAT);
        predefinedTypes.put("double", DOUBLE);
        predefinedTypes.put("boolean", BOOLEAN);
        predefinedTypes.put("char", CHAR);
    }

    static Collection<String> getPredefinedTypes() {
        return predefinedTypes.keySet();
    }

    /**
     * Builds a domain tree from excel rules project meta information.
     *
     * @param projectOpenClass project open class.
     * @return <code>DomainTree</code> instance
     */
    public static DomainTree buildTree(IOpenClass projectOpenClass) {
        if (projectOpenClass instanceof NullOpenClass) {
            // it means module wasn`t loaded.
            //
            throw new IllegalArgumentException("Module is corrupted.");
        }
        IMetaInfo projectInfo = projectOpenClass.getMetaInfo();

        if (!(projectInfo instanceof XlsMetaInfo)) {
            throw new IllegalArgumentException("Only XlsMetaInfo is currenty supported");
        }
        DomainTree domainTree = new DomainTree();

        // Add all datatypes
        for (IOpenClass type : projectOpenClass.getTypes()) {
            domainTree.addType(type);
        }

        Map<String, IOpenClass> projectTypes = domainTree.treeElements;
        domainTree.treeElements = new LinkedHashMap<>(predefinedTypes);
        domainTree.treeElements.putAll(projectTypes);
        return domainTree;
    }

    /**
     * Private constructor, it prevents direct instantiaion of the class.
     */
    private DomainTree() {
        treeElements = new TreeMap<>();
    }

    private void addType(IOpenClass type) {
        if (type instanceof ComponentTypeArrayOpenClass) {
            addType(type.getComponentClass()); // Add component class only
            return;
        }

        if (type instanceof SpreadsheetOpenClass || type instanceof CustomSpreadsheetResultOpenClass) {
            // Do not process SpreadsheetResult types
            return;
        }

        String simpleTypeName = type.getDisplayName(INamedThing.SHORT);
        if (predefinedTypes.containsKey(simpleTypeName) || ignoredTypes.contains(simpleTypeName)) {
            // Already predefined
            return;
        }

        if (ClassUtils.isAssignable(type.getInstanceClass(), Collection.class)) {
            // Do not process aggregated types
            return;
        }

        treeElements.putIfAbsent(simpleTypeName, type);

        // types of IOpenClass fields
        Map<String, IOpenField> fields = type.getFields();
        for (IOpenField field : fields.values()) {
            if (!field.isStatic() && !field.getType().isAbstract()) {
                addType(field.getType());
            }
        }
    }

    public Collection<IOpenClass> getAllOpenClasses() {
        return new ArrayList<>(treeElements.values());
    }

    public Collection<String> getAllClasses() {
        return new ArrayList<>(treeElements.keySet());
    }

}
