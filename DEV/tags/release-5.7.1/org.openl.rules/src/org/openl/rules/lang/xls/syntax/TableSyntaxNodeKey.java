package org.openl.rules.lang.xls.syntax;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodKey;

/**
 * Key to check identity of tables represented by TableSyntaxNodes. Tables are
 * identical when they have the same method signature and the same business
 * dimension properties.
 * 
 * @author PUdalau
 */
public class TableSyntaxNodeKey {
    private TableSyntaxNode tsn;

    public TableSyntaxNodeKey(TableSyntaxNode tsn) {
        this.tsn = tsn;
    }

    /**
     * @return The TableSyntaxNode, key was generated for 
     */
    public TableSyntaxNode getTableSyntaxNode() {
        return tsn;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TableSyntaxNodeKey)) {
            return false;
        }
        TableSyntaxNodeKey key = (TableSyntaxNodeKey) obj;

        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(new MethodKey((IOpenMethod) tsn.getMember()), new MethodKey((IOpenMethod) key.getTableSyntaxNode()
                .getMember()));
        String[] dimensionalPropertyNames = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
        for (int i = 0; i < dimensionalPropertyNames.length; i++) {
            equalsBuilder.append(tsn.getTableProperties().getPropertyValue(dimensionalPropertyNames[i]), 
                    key.getTableSyntaxNode().getTableProperties().getPropertyValue(dimensionalPropertyNames[i]));
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(new MethodKey((IOpenMethod) tsn.getMember()));
        String[] dimensionalPropertyNames = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
        for (int i = 0; i < dimensionalPropertyNames.length; i++) {
            hashCodeBuilder.append(tsn.getTableProperties().getPropertyValue(dimensionalPropertyNames[i]));
        }
        return hashCodeBuilder.toHashCode();
    }
    
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(new MethodKey((IOpenMethod) tsn.getMember()));
        String[] dimensionalPropertyNames = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
        stringBuilder.append('[');
        for (int i = 0; i < dimensionalPropertyNames.length; i++) {
            if(i!= 0){
                stringBuilder.append(',');
            }
            stringBuilder.append(dimensionalPropertyNames[i]).append('=');
            stringBuilder.append(tsn.getTableProperties().getPropertyValueAsString(dimensionalPropertyNames[i]));
        }
        return stringBuilder.append(']').toString();
    }
}
