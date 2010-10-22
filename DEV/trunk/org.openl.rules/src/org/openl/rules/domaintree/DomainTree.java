package org.openl.rules.domaintree;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import org.openl.base.INamedThing;
import org.openl.meta.DoubleValue;
import org.openl.meta.IMetaInfo;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ArrayOpenClass;
import org.openl.types.java.JavaOpenClass;

import static org.openl.types.java.JavaOpenClass.*;

/**
 * @author Aliaksandr Antonik.
 */
public class DomainTree {
    private static final Set<String> ignoredTypes;
    private static Map<String, IOpenClass> predefinedTypes = new TreeMap<String, IOpenClass>();

    private final Map<String, IOpenClass> treeElements;

    static {
        ignoredTypes = new HashSet<String>();
        ignoredTypes.add(OBJECT.getSimpleName());
        ignoredTypes.add(CLASS.getSimpleName());
        ignoredTypes.add(VOID.getSimpleName());
    }

    static {
        predefinedTypes = new HashMap<String, IOpenClass>();

        //primitives
        predefinedTypes.put(INT.getSimpleName(), INT);
        predefinedTypes.put(BOOLEAN.getSimpleName(), BOOLEAN);
        predefinedTypes.put(LONG.getSimpleName(), LONG);
        predefinedTypes.put(DOUBLE.getSimpleName(), DOUBLE);
        predefinedTypes.put(FLOAT.getSimpleName(), FLOAT);
        predefinedTypes.put(SHORT.getSimpleName(), SHORT);
        predefinedTypes.put(CHAR.getSimpleName(), CHAR);

        // wrappers for primitives
//      predefinedTypes.put("Integer", JavaOpenClass.getOpenClass(Integer.class));
//      predefinedTypes.put("Boolean", JavaOpenClass.getOpenClass(Boolean.class));
//      predefinedTypes.put("Long", JavaOpenClass.getOpenClass(Long.class));
//      predefinedTypes.put("Double", JavaOpenClass.getOpenClass(Double.class));
//      predefinedTypes.put("Float", JavaOpenClass.getOpenClass(Float.class));
//      predefinedTypes.put("Short", JavaOpenClass.getOpenClass(Short.class));
//      predefinedTypes.put("Character", JavaOpenClass.getOpenClass(Character.class));

        predefinedTypes.put(STRING.getSimpleName(), STRING);
        predefinedTypes.put("Date", JavaOpenClass.getOpenClass(Date.class));
        predefinedTypes.put("BigInteger", JavaOpenClass.getOpenClass(BigInteger.class));
        predefinedTypes.put("BigDecimal", JavaOpenClass.getOpenClass(BigDecimal.class));
        predefinedTypes.put("IntRange", JavaOpenClass.getOpenClass(IntRange.class));
        predefinedTypes.put("DoubleRange", JavaOpenClass.getOpenClass(DoubleRange.class));
        predefinedTypes.put("DoubleValue", JavaOpenClass.getOpenClass(DoubleValue.class));
    }

    /**
     * Builds a domain tree from excel rules project meta information.
     *
     * @param projectOpenClass project open class.
     * @return <code>DomainTree</code> instance
     */
    public static DomainTree buildTree(IOpenClass projectOpenClass, boolean addDatatypes) {
        if (projectOpenClass instanceof NullOpenClass) {
            // it means module wasn`t loaded.
            //
            throw new IllegalArgumentException("Module is corrupted.");
        }
        IMetaInfo projectInfo = projectOpenClass.getMetaInfo();
        
        if (projectInfo instanceof XlsMetaInfo) {
            DomainTree domainTree = new DomainTree();

            if (addDatatypes) {
                // Add all datatypes
                Map<String, IOpenClass> dataTypes = projectOpenClass.getTypes();
                if (dataTypes.size() > 0) {
                    for (IOpenClass type : dataTypes.values()) {
                        domainTree.addType(type);
                    }
                }
            }
            return domainTree;
        } else {
            throw new IllegalArgumentException("Only XlsMetaInfo is currenty supported");
        }
    }

    public static DomainTree buildTree(IOpenClass projectOpenClass) {
        return buildTree(projectOpenClass, true);
    }

    private static boolean inspectTypeRecursively(IOpenClass type) {
        return !type.isSimple();
    }

    private static boolean isAppropriateProperty(IOpenField field) {
        return !field.isStatic() && !field.getType().isAbstract();
    }
    
    private boolean isArrayType(IOpenClass fieldType) {
        return fieldType instanceof ArrayOpenClass;
    }

    /**
     * Private constructor, it prevents direct instantiaion of the class.
     */
    private DomainTree() {
        treeElements = new HashMap<String, IOpenClass>(predefinedTypes);
    }

    private boolean addType(IOpenClass type) {
        if (isArrayType(type)) {
            type = getComponentType(type);
        }
                
        String simpleTypeName = type.getDisplayName(INamedThing.SHORT);

        if (!treeElements.containsKey(simpleTypeName) && !ignoredTypes.contains(simpleTypeName)) {
            Class<?> instanceClass = type.getInstanceClass(); // instance class can be null, in case 
                                                              // the are errors in datatype table. it cause stop 
                                                              // processing datatype table binding.
            
            if (instanceClass != null && Collection.class.isAssignableFrom(instanceClass)) {
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
        IOpenClass result = null;
        if (isArrayType(type)) {
            IOpenClass componentType = ((ArrayOpenClass)type).getComponentClass();
            if (componentType != null) {
                result = componentType;
            } else {
                result = type;
            }
        } 
        return result;
    }

    /**
     * Flat list of class names.
     *
     * @param sorted if <code>true</code> returned collection is sorted,
     *            otherwise name order is not specified.
     * @return all class names of the domain tree
     */
    public Collection<String> getAllClasses(boolean sorted) {
        Collection<String> unsortedClasses = treeElements.keySet();
        if (sorted) {
            List<String> sortedClasses = new ArrayList<String>(unsortedClasses);
            Collections.sort(sortedClasses, new Comparator<String>() {
                public int compare(String s1, String s2) {
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
                }
            });

            return sortedClasses;
        } else {
            return unsortedClasses;
        }
    }

    /**
     * Returns properties of a given class.
     *
     * @param typename class to get properties for.
     * @return collection of property names or <code>null</code> if typename
     *         is unknown.
     */
    public Collection<String> getClassProperties(String typename) {
        IOpenClass openClass = treeElements.get(typename);
        if (openClass == null) {
            return null;
        }

        Collection<String> result = new ArrayList<String>();
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
        return openClass.getName();
    }

}
