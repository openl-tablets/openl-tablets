package org.openl.rules.datatype.binding;

import static org.openl.rules.datatype.binding.DatatypeTableBoundNode.canProcessRow;
import static org.openl.rules.datatype.binding.DatatypeTableBoundNode.getCellSource;
import static org.openl.rules.datatype.binding.DatatypeTableBoundNode.getIdentifierNode;

import java.util.LinkedHashSet;
import java.util.Set;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.util.StringUtils;

/**
 * In the Datatype TableSyntaxNode find the dependent types.
 * There are 2 types of dependencies:
 * 1) inheritance dependency (TypeA extends TypeB)
 * 2) dependency in field declaration (TypeB fieldB)
 *
 * @author Denis Levchuk
 */
class DependentTypesExtractor {
    public static final String ALIASDATATYPE_PATTERN = "^.+\\<.+\\>\\s*$";
    
    private boolean isAliasDatatype(TableSyntaxNode node){
        String header = node.getHeader().getSourceString();
        return header.matches(ALIASDATATYPE_PATTERN);
    }
    
    public Set<String> extract(TableSyntaxNode node, IBindingContext cxt) {
        ILogicalTable dataPart = DatatypeHelper.getNormalizedDataPartTable(
                node.getTable(),
                OpenL.getInstance(OpenL.OPENL_JAVA_NAME),
                cxt);

        int tableHeight = 0;

        if (dataPart != null) {
            tableHeight = dataPart.getHeight();
        }

        Set<String> dependencies = new LinkedHashSet<>();
        if (isAliasDatatype(node)){
            //Alias datatype doens't have dependencies
            return dependencies;
        }
        String parentType = getParentDatatypeName(node);
        if (StringUtils.isNotBlank(parentType)) {
            dependencies.add(parentType);
        }

        for (int i = 0; i < tableHeight; i++) {
            ILogicalTable row = dataPart.getRow(i);

            if (canProcessRow(row, cxt)) {
                String typeName = getType(row, cxt);
                if (StringUtils.isNotBlank(typeName)) {
                    dependencies.add(typeName);
                }
            }

        }
        return dependencies;
    }

    private String getParentDatatypeName(TableSyntaxNode tsn) {

        if (XlsNodeTypes.XLS_DATATYPE.equals(tsn.getNodeType())) {
            IOpenSourceCodeModule src = tsn.getHeader().getModule();

            IdentifierNode[] parsedHeader = new IdentifierNode[0];
            try {
                parsedHeader = DatatypeHelper.tokenizeHeader(src);
            } catch (OpenLCompilationException e) {
                // Suppress the exception
                // This exception has already been processed when parsing the table header
                //
            }

            if (parsedHeader.length == 4) {
                return parsedHeader[DatatypeNodeBinder.PARENT_TYPE_INDEX].getIdentifier();
            } else {
                return null;
            }
        }

        return null;
    }

    private String getType(ILogicalTable row, IBindingContext cxt) {
        // Get the cell that has index 0. This cell contains the Type name
        //
        GridCellSourceCodeModule typeSrc = getCellSource(row, cxt, 0);
        IdentifierNode[] idn = new IdentifierNode[0];
        try {
            idn = getIdentifierNode(typeSrc);
        } catch (OpenLCompilationException e) {
            // Suppress the exception
            //
        }
        if (idn.length >= 1) {
            String type = idn[0].getIdentifier();
            if (type.contains("[")) {
                // Use the clean type, without array declarations
                //
                type = type.substring(0, type.indexOf("["));
            }
            // Return the Type name
            //
            return type;
        }
        // Alias Datatype don't have Type name
        //
        return null;
    }
}
