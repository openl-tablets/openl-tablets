package org.openl.rules.lang.xls.syntax;

import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.properties.DimensionPropertiesMethodKey;

/**
 * Key to check identity of tables represented by TableSyntaxNodes. Tables are identical when they have the same method
 * signature and the same business dimension properties.
 *
 * @author PUdalau
 */
public class TableSyntaxNodeKey {
    private TableSyntaxNode tsn;
    private DimensionPropertiesMethodKey methodKey;

    public TableSyntaxNodeKey(TableSyntaxNode tsn) {
        this.tsn = tsn;
        this.methodKey = new DimensionPropertiesMethodKey((ExecutableRulesMethod) tsn.getMember());
    }

    /**
     * @return The TableSyntaxNode, key was generated for
     */
    public TableSyntaxNode getTableSyntaxNode() {
        return tsn;
    }

    /**
     *
     * @return {@link DimensionPropertiesMethodKey} for {@link TableSyntaxNode} member.
     */
    public DimensionPropertiesMethodKey getMethodKey() {
        return methodKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TableSyntaxNodeKey)) {
            return false;
        }
        TableSyntaxNodeKey key = (TableSyntaxNodeKey) obj;

        return methodKey.equals(key.getMethodKey());
    }

    @Override
    public int hashCode() {
        return methodKey.hashCode();
    }

    @Override
    public String toString() {
        return methodKey.toString();
    }
}
