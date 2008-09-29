package org.openl.rules.domaintree;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import org.openl.meta.IMetaInfo;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IDTAction;
import org.openl.rules.dt.IDTCondition;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IParameterDeclaration;
import static org.openl.types.java.JavaOpenClass.*;

/**
 * @author Aliaksandr Antonik.
 */
public class DomainTree {
    private final Map<String, IOpenClass> treeElements;
    private static final Set<String> ignoredTypes;

    static {
        ignoredTypes = new HashSet<String>();
        ignoredTypes.add("java.lang.Object");
        ignoredTypes.add("java.lang.Class");
        ignoredTypes.add("void");
    }

    private static Map<String, IOpenClass> predefinedTypes = new TreeMap<String, IOpenClass>();

    static {
        predefinedTypes = new HashMap<String, IOpenClass>();
        predefinedTypes.put(INT.getName(), INT);
        predefinedTypes.put(STRING.getName(), STRING);
        predefinedTypes.put(BOOLEAN.getName(), BOOLEAN);
        predefinedTypes.put(LONG.getName(), LONG);
        predefinedTypes.put(DOUBLE.getName(), DOUBLE);
        predefinedTypes.put(FLOAT.getName(), FLOAT);
        predefinedTypes.put(SHORT.getName(), SHORT);
        predefinedTypes.put(CHAR.getName(), CHAR);
    }

    /**
     * Private constructor, it prevents direct instantiaion of the class.
     */
    private DomainTree() {
        treeElements = new HashMap<String, IOpenClass>(predefinedTypes);
    }

    /**
     * Builds a domain tree from excel rules project meta information.
     *
     * @param projectInfo project meta inforamtion.
     * @return <code>DomainTree</code> instance
     */
    public static DomainTree buildTree(IMetaInfo projectInfo) {
        if (projectInfo == null) {
            throw new NullPointerException("projectInfo is null");
        }

        if (projectInfo instanceof XlsMetaInfo) {
            DomainTree domainTree = new DomainTree();

            XlsMetaInfo xlsMetaInfo = (XlsMetaInfo) projectInfo;
            for (TableSyntaxNode node : xlsMetaInfo.getXlsModuleNode().getXlsTableSyntaxNodes()) {
                if (node.getType().equals(ITableNodeTypes.XLS_DT)) {
                    domainTree.scanTable((DecisionTable) node.getMember());
                }
            }

            return domainTree;
        } else {
            throw new IllegalArgumentException("Only XlsMetaInfo is currenty supported");
        }
    }

    /**
     * Scans given table, and adds classes that the table references (parameter types,
     * condition and action variable types) to the tree.
     *
     * @param decisionTable decision table to scan.
     */
    private void scanTable(DecisionTable decisionTable) {
        for (IOpenClass paramType : decisionTable.getHeader().getSignature().getParameterTypes()) {
            addType(paramType);
        }

        for (IDTCondition condition : decisionTable.getConditionRows()) {
            for (IParameterDeclaration param : condition.getParams()) {
                addType(param.getType());
            }
        }

        for (IDTAction action : decisionTable.getActionRows()) {
            for (IParameterDeclaration param : action.getParams()) {
                addType(param.getType());
            }
        }
    }

    private boolean addType(IOpenClass type) {
        if (!treeElements.containsKey(type.getName()) && !ignoredTypes.contains(type.getName())) {
            if (Collection.class.isAssignableFrom(type.getInstanceClass()))
                return false;

            treeElements.put(type.getName(), type);

            if (inspectTypeRecursively(type)) {
                // types of IOpenClass fields
                Iterator<IOpenField> fieldIterator = type.fields();
                while (fieldIterator.hasNext()) {
                    IOpenField field = fieldIterator.next();
                    if (isAppropriateProperty(field)) {
                        addType(field.getType());
                    }
                }
            }

            return true;
        }

        return false;
    }

    private static boolean isAppropriateProperty(IOpenField field) {
        return !field.isStatic() && !field.getType().isAbstract();
    }

    private static boolean inspectTypeRecursively(IOpenClass type) {
        return !type.isSimple();
    }

    /**
     * Flat list of class names.
     *
     * @param sorted if <code>true</code> returned collection is sorted, otherwise name order is not specified.
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
                            if (primitive1)
                                return defPackage2 ? -1 : 1;
                            return defPackage1 ? -1 : 1;
                        }
                        return s1.compareTo(s2);
                    }
                    return primitive1 ? -1 : 1;
                }
            });

            return sortedClasses;
        } else
            return unsortedClasses;
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

        Collection<String> result = new ArrayList<String>();
        Iterator<IOpenField> fieldIterator = openClass.fields();
        while (fieldIterator.hasNext()) {
            IOpenField field = fieldIterator.next();
            if (isAppropriateProperty(field)) {
                result.add(field.getName());
            }
        }
        return result;
    }

    public String getTypename(DomainTreeContext context, String path) {
        if (path == null)
            return null;
        String[] parts = path.split("\\.");
        if (parts.length == 0)
            return null;
        String rootClassname = context.getObjectType(parts[0]);
        if (rootClassname == null)
            return null;
        IOpenClass openClass = treeElements.get(rootClassname);
        if (openClass == null)
            return null;

        for (int i = 1; i < parts.length; ++i) {
            IOpenField field = openClass.getField(parts[i]);
            if (field == null)
                return null;
            openClass = field.getType();
        }
        return openClass.getName();
    }
}
