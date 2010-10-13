package org.openl.rules.datatype.binding;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.BindHelper;
import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.util.ArrayTool;

public class DatatypeHelper {

    public static IDomain<?> getTypeDomain(ILogicalTable table, IOpenClass type, OpenL openl, IBindingContext cxt)
        throws SyntaxNodeException {
        Object values = loadAliasDatatypeValues(table, type, openl, cxt);

        if (values != null) {
            return new EnumDomain(ArrayTool.toArray(values));
        }

        return new EnumDomain<Object>(new Object[] {});
    }

    public static Object loadAliasDatatypeValues(ILogicalTable table, IOpenClass type, OpenL openl, IBindingContext cxt)
        throws SyntaxNodeException {

        OpenlToolAdaptor openlAdaptor = new OpenlToolAdaptor(openl, cxt);

        return RuleRowHelper.loadParam(table, type, "Values", "", openlAdaptor, true);
    }

//    public static boolean isAliasDatatype(ILogicalTable table, OpenL openl, IBindingContext cxt) {
//
//        ILogicalTable dataPart = getNormalizedDataPartTable(table, openl, cxt);
//
//        int height = dataPart.getHeight();
//        int typesCount1 = countTypes(dataPart, openl, cxt);
//        int typesCount2 = countTypes(dataPart.transpose(), openl, cxt);
//        int width = dataPart.getWidth();
//
//        if (typesCount1 == 0 && typesCount2 == 0 && (height == 0 // values are
//                // not provided
//                || width == 1 || height == 1)) {
//            return true;
//        }
//
//        return false;
//    }

    public static ILogicalTable getNormalizedDataPartTable(ILogicalTable table, OpenL openl, IBindingContext cxt) {
        
        ILogicalTable dataPart = null;
        if (PropertiesHelper.getPropertiesTableSection(table) != null) {
             dataPart = table.getRows(2);
        } else {
            dataPart = table.getRows(1);
        }
        
        //if datatype table has only one row
        if (dataPart.getHeight() == 1) {
            return dataPart;
        } else if (dataPart.getWidth() == 1) {
            return dataPart.transpose();
        }

        int verticalCount = countTypes(dataPart, openl, cxt);
        int horizontalCount = countTypes(dataPart.transpose(), openl, cxt);

        if (verticalCount < horizontalCount) {
            return dataPart.transpose();
        }

        return dataPart;
    }

    private static int countTypes(ILogicalTable table, OpenL openl, IBindingContext cxt) {

        int height = table.getHeight();
        int count = 0;

        for (int i = 0; i < height; ++i) {
            try {
                IOpenClass type = makeType(table.getRow(i), openl, cxt);
                if (type != null) {
                    count += 1;
                }
            } catch (Throwable t) {
                // Ignore exception.
                int a = 1;
            }
        }

        return count;
    }

    private static IOpenClass makeType(ILogicalTable table, OpenL openl, IBindingContext cxt) {

        GridCellSourceCodeModule source = new GridCellSourceCodeModule(table.getSource(), cxt);

        return OpenLManager.makeType(openl, source, (IBindingContextDelegator) cxt);
    }

    private static String getDatatypeName(TableSyntaxNode tsn) throws OpenLCompilationException {

        if (ITableNodeTypes.XLS_DATATYPE.equals(tsn.getType())) {
            IOpenSourceCodeModule src = tsn.getHeader().getModule();

            IdentifierNode[] parsedHeader = Tokenizer.tokenize(src, " \n\r");

            return parsedHeader[DatatypeNodeBinder.TYPE_INDEX].getIdentifier();
        }

        return null;
    }

    private static String getParentDatatypeName(TableSyntaxNode tsn) throws OpenLCompilationException {

        if (ITableNodeTypes.XLS_DATATYPE.equals(tsn.getType())) {
            IOpenSourceCodeModule src = tsn.getHeader().getModule();

            IdentifierNode[] parsedHeader = Tokenizer.tokenize(src, " \n\r");

            if (parsedHeader.length == 4) {
                return parsedHeader[DatatypeNodeBinder.PARENT_TYPE_INDEX].getIdentifier();
            } else {
                return null;
            }
        }

        return null;
    }

    public static Map<String, TableSyntaxNode> createTypesMap(TableSyntaxNode[] nodes, IBindingContext bindingContext) {

        Map<String, TableSyntaxNode> map = new HashMap<String, TableSyntaxNode>();

        if (nodes == null) {
            return map;
        }

        for (TableSyntaxNode tsn : nodes) {

            if (ITableNodeTypes.XLS_DATATYPE.equals(tsn.getType())) {
                
                try {
                    String datatypeName = DatatypeHelper.getDatatypeName(tsn);

                    if (datatypeName != null) {
                        if (map.containsKey(datatypeName)) {
                            String message = String.format("Type with name '%s' already exists", datatypeName);
                            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, tsn);
                            tsn.addError(error);
                            BindHelper.processError(error);
                        } else {
                            map.put(datatypeName, tsn);
                        }
                    } else {
                        String message = "Cannot recognize type name";
                        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, tsn);
                        tsn.addError(error);
                        BindHelper.processError(error);
                    }
                } catch (OpenLCompilationException e) {
                    String message = "An error has occurred during compilation";
                    SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, e, tsn);
                    tsn.addError(error);
                    BindHelper.processError(error);
                }
            }
        }

        return map;
    }

    public static TableSyntaxNode[] orderDatatypes(Map<String, TableSyntaxNode> typesMap,
            IBindingContext bindingContext) {
        
        Map<TableSyntaxNode, Integer> levelsMap = new HashMap<TableSyntaxNode, Integer>();
        
        for (TableSyntaxNode node: typesMap.values()) {
            try {
                int level = getInheritanceLevel(typesMap, node);
                levelsMap.put(node, level);
            } catch (OpenLCompilationException e) {
                String message = "An error has occurred during compilation";
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, e, node);
                node.addError(error);
                BindHelper.processError(error);
            }
        }

        Set<TableSyntaxNode> nodes = levelsMap.keySet();
        TableSyntaxNode[] nodesToOrder = nodes.toArray(new TableSyntaxNode[nodes.size()]);
        DatatypeNodeLevelComparator comparator = new DatatypeNodeLevelComparator(levelsMap);
        Arrays.sort(nodesToOrder, comparator);
        
        return nodesToOrder;
    }
    
    public static int getInheritanceLevel(Map<String, TableSyntaxNode> types, TableSyntaxNode tsn)
        throws OpenLCompilationException {
        return getInheritanceLevel(types, tsn, new LinkedHashMap<String, TableSyntaxNode>());
    }
    
    private static int getInheritanceLevel(
            Map<String, TableSyntaxNode> types, TableSyntaxNode tsn, Map<String, TableSyntaxNode> children)
        throws OpenLCompilationException {
        
        String parent = getParentDatatypeName(tsn);
        
        if (parent != null && types.containsKey(parent)) {
            if (children.containsKey(parent)) {
                Set<String> keys = children.keySet();
                String typesCycle = StringUtils.join(keys, " -> ");

                throw new OpenLCompilationException(String.format("Invalid type hierarchy found: %s", typesCycle));
            }
            
            children.put(parent, tsn);
            
            return 1 + getInheritanceLevel(types, types.get(parent), children);
        } else {
            return 0;
        }
    }

}
