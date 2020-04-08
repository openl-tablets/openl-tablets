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
import java.util.*;

import org.apache.commons.lang.ClassUtils;
import org.openl.base.INamedThing;
import org.openl.meta.IMetaInfo;
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

    private final Map<String, IOpenClass> treeElements;

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

        if (projectInfo instanceof XlsMetaInfo) {
            DomainTree domainTree = new DomainTree();

            // Add all datatypes
            for (IOpenClass type : projectOpenClass.getTypes()) {
                domainTree.addType(type);
            }
            return domainTree;
        } else {
            throw new IllegalArgumentException("Only XlsMetaInfo is currenty supported");
        }
    }

    private static boolean inspectTypeRecursively(IOpenClass type) {
        return !type.isSimple();
    }

    private static boolean isAppropriateProperty(IOpenField field) {
        return !field.isStatic() && !field.getType().isAbstract();
    }

    private boolean isArrayType(IOpenClass fieldType) {
        return fieldType instanceof ComponentTypeArrayOpenClass;
    }

    /**
     * Private constructor, it prevents direct instantiaion of the class.
     */
    private DomainTree() {
        treeElements = new HashMap<>(predefinedTypes);
    }

    private boolean addType(IOpenClass type) {
        type = getComponentType(type);

        String simpleTypeName = type.getDisplayName(INamedThing.SHORT);

        if (!treeElements.containsKey(simpleTypeName) && !ignoredTypes.contains(simpleTypeName)) {
            Class<?> instanceClass = type.getInstanceClass(); // instance class can be null, in case
            // the are errors in datatype table. it cause stop
            // processing datatype table binding.

            if (ClassUtils.isAssignable(instanceClass, Collection.class)) {
                return false;
            }

            treeElements.put(simpleTypeName, type);

            if (inspectTypeRecursively(type)) {
                // types of IOpenClass fields
                Map<String, IOpenField> fields = type.getFields();
                for (IOpenField field : fields.values()) {
                    if (isAppropriateProperty(field)) {
                        addType(field.getType());
                    }
                }
            }

            return true;
        }

        return false;
    }

    private IOpenClass getComponentType(IOpenClass type) {
        if (isArrayType(type)) {
            IOpenClass componentType = type.getComponentClass();
            return componentType == null ? type : componentType;
        } else {
            return type;
        }
    }

    /**
     * Flat list of class names.
     *
     * @return sorted collection of the domain tree
     */
    public Collection<IOpenClass> getAllOpenClasses() {
        Collection<IOpenClass> unsortedClasses = treeElements.values();
        List<IOpenClass> sortedClasses = new ArrayList<>(unsortedClasses);
        sortedClasses.sort(Comparator.comparing(s -> s.getDisplayName(INamedThing.SHORT)));
        return sortedClasses;
    }

    public Collection<String> getAllClasses() {
        Collection<String> unsortedClasses = treeElements.keySet();
        List<String> sortedClasses = new ArrayList<>(unsortedClasses);
        Collections.sort(sortedClasses, (s1, s2) -> {
            boolean primitive1 = predefinedTypes.containsKey(s1);
            boolean primitive2 = predefinedTypes.containsKey(s2);
            if (primitive1 == primitive2) {
                boolean defPackage1 = s1.startsWith("java.");
                boolean defPackage2 = s2.startsWith("java.");
                if (defPackage1 != defPackage2) {
                    if (primitive1) {
                        return defPackage2 ? -1 : 1;
                    }
                    return defPackage1 ? -1 : 1;
                }
                return s1.compareTo(s2);
            }
            return primitive1 ? -1 : 1;
        });

        return sortedClasses;
    }

    /**
     * Returns properties of a given class.
     *
     * @param typename class to get properties for.
     * @return collection of property names or <code>null</code> if typename is unknown.
     */
    public Collection<String> getClassProperties(String typename) {
        IOpenClass openClass = treeElements.get(typename);
        if (openClass == null) {
            return null;
        }

        Collection<String> result = new ArrayList<>();
        Map<String, IOpenField> fields = openClass.getFields();
        for (IOpenField field : fields.values()) {
            if (isAppropriateProperty(field)) {
                result.add(field.getName());
            }
        }
        return result;
    }

    public String getTypename(DomainTreeContext context, String path) {
        if (path == null) {
            return null;
        }
        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return null;
        }
        String rootClassname = context.getObjectType(parts[0]);
        if (rootClassname == null) {
            return null;
        }
        IOpenClass openClass = treeElements.get(rootClassname);
        if (openClass == null) {
            return null;
        }

        for (int i = 1; i < parts.length; ++i) {
            IOpenField field = openClass.getField(parts[i]);
            if (field == null) {
                return null;
            }
            openClass = field.getType();
        }
        return openClass.getDisplayName(INamedThing.SHORT);
    }

}
