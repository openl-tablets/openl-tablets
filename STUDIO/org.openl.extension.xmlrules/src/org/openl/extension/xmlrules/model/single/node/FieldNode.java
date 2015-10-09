package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "field-node")
public class FieldNode extends ChainedNode {
    private String fieldName;

    @XmlElement(name = "field-name", required = true)
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String toOpenLString() {
        return toOpenLString(false, 0);
    }

    public String toOpenLString(boolean skipFilters, int skipFieldsCount) {
        Node node = getNode();

        String obj;
        int nextSkipFieldsCount = skipFieldsCount == 0 ? 0 : skipFieldsCount - 1;
        if (node instanceof FieldNode) {
            obj = ((FieldNode) node).toOpenLString(skipFilters, nextSkipFieldsCount);
        } else if (node instanceof FilterNode) {
            obj = ((FilterNode) node).toOpenLString(skipFilters, nextSkipFieldsCount);
            if (!skipFilters) {
                obj = ((FilterNode) node).wrapWithFieldAccess(obj, false, nextSkipFieldsCount);
            }
        } else if (node instanceof ParentNode) {
            throw new UnsupportedOperationException("Can't get field from Parent node");
        } else {
            obj = node.toOpenLString();
        }

        return skipFieldsCount > 0 ? obj : "Field(" + obj + ", \"" + fieldName + "\")";
    }
}
