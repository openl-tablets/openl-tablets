package org.openl.rules.cmatch;

import java.util.List;

import org.openl.meta.StringValue;

@Deprecated
public class ColumnMatchTreeNode {

    private StringValue name;
    private StringValue operation;
    private StringValue[] values;
    List<ColumnMatchTreeNode> children;
    ColumnMatchTableParserSpecification specification;

    /**
     * @return the name
     */
    public StringValue getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(StringValue name) {
        this.name = name;
    }
    /**
     * @return the operation
     */
    public StringValue getOperation() {
        return operation;
    }
    /**
     * @param operation the operation to set
     */
    public void setOperation(StringValue operation) {
        this.operation = operation;
    }
    /**
     * @return the values
     */
    public StringValue[] getValues() {
        return values;
    }
    /**
     * @param values the values to set
     */
    public void setValues(StringValue[] values) {
        this.values = values;
    }
    /**
     * @return the children
     */
    public List<ColumnMatchTreeNode> getChildren() {
        return children;
    }
    /**
     * @param children the children to set
     */
    public void setChildren(List<ColumnMatchTreeNode> children) {
        this.children = children;
    }
    /**
     * @return the specification
     */
    public ColumnMatchTableParserSpecification getSpecification() {
        return specification;
    }
    /**
     * @param specification the specification to set
     */
    public void setSpecification(ColumnMatchTableParserSpecification specification) {
        this.specification = specification;
    }
    
}
