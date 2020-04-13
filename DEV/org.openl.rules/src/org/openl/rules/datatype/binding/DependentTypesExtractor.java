package org.openl.rules.datatype.binding;

import java.util.LinkedHashSet;
import java.util.Set;

import org.openl.binding.IBindingContext;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.util.StringUtils;

/**
 * In the Datatype TableSyntaxNode find the dependent types. There are 2 types of dependencies: 1) inheritance
 * dependency (TypeA extends TypeB) 2) dependency in field declaration (TypeB fieldB)
 *
 * @author Denis Levchuk
 */
class DependentTypesExtractor {
    public static final String ALIAS_DATATYPE_PATTERN = "^.+\\<.+\\>\\s*$";

    private boolean isAliasDatatype(TableSyntaxNode node) {
        String header = node.getHeader().getSourceString();
        return header.matches(ALIAS_DATATYPE_PATTERN);
    }

    public Set<String> extract(TableSyntaxNode node, IBindingContext cxt) {
        Set<String> dependencies = new LinkedHashSet<>();
        if (isAliasDatatype(node)) {
            // Alias datatype doens't have dependencies
            return dependencies;
        }
        String parentType = getParentDatatypeName(node);
        if (StringUtils.isNotBlank(parentType)) {
            dependencies.add(parentType);
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
}
