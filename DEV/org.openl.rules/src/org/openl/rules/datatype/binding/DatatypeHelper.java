package org.openl.rules.datatype.binding;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.lang.xls.XlsNodeTypes;
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

    @SuppressWarnings("unchecked")
    public static IDomain<?> getTypeDomain(ILogicalTable table, IOpenClass type, OpenL openl, IBindingContext cxt)
        throws SyntaxNodeException {
        if (table != null) {
            Object values = loadAliasDatatypeValues(table, type, openl, cxt);

            if (values != null) {
                return new EnumDomain(ArrayTool.toArray(values));
            }
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

        if (dataPart == null) {
            return null;
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
            }
        }

        return count;
    }

    private static IOpenClass makeType(ILogicalTable table, OpenL openl, IBindingContext cxt) {

        GridCellSourceCodeModule source = new GridCellSourceCodeModule(table.getSource(), cxt);

        return OpenLManager.makeType(openl, source, (IBindingContextDelegator) cxt);
    }

    /**
     * TODO: This method should be generic for the TableSyntaxNode
     * and return the type of the table
     * e.g. TableSyntaxNode.getTableReturnType()
     */
    public static String getDatatypeName(TableSyntaxNode tsn) throws OpenLCompilationException {

        if (XlsNodeTypes.XLS_DATATYPE.equals(tsn.getNodeType())) {
            IOpenSourceCodeModule src = tsn.getHeader().getModule();

            IdentifierNode[] parsedHeader = tokenizeHeader(src);

            return parsedHeader[DatatypeNodeBinder.TYPE_INDEX].getIdentifier();
        }

        return null;
    }
    
    public static IdentifierNode[] tokenizeHeader(IOpenSourceCodeModule tableHeader) throws OpenLCompilationException {
        IdentifierNode[] parsedHeader = Tokenizer.tokenize(tableHeader, " \n\r");
        if (parsedHeader.length < 2) {
            String message = "Datatype table format: Datatype <typename>";
            throw SyntaxNodeExceptionUtils.createError(message, null, null, tableHeader);
        }
        return parsedHeader;
    }

}
