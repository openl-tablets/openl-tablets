package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlType;

@XmlType(name = "parent-node")
public class ParentNode extends ChainedNode {
    @Override
    public String toOpenLString() {
        int parentCount = 1;
        Node node = getNode();
        while (node instanceof ParentNode) {
            node = ((ParentNode) node).getNode();
            parentCount++;
        }

        if (node instanceof FieldNode) {
            return ((FieldNode) node).toOpenLString(false, parentCount);
        } else if (node instanceof FilterNode) {
            FilterNode filterNode = (FilterNode) node;
            String filterString = filterNode.toOpenLString(false, 0);
            return filterNode.wrapWithFieldAccess(filterString, true, parentCount);
        }

        throw new IllegalArgumentException("Can't apply Parent() to the node");
    }
}
