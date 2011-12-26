package org.openl.rules.webstudio.web.test;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.openl.base.INameSpacedThing;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaOpenClass;

public class CollectionParameterTreeNode extends ParameterDeclarationTreeNode {
    public static final String COLLECTION_TYPE = "collection";

    public CollectionParameterTreeNode(String fieldName, Object value, IOpenClass fieldType, ParameterDeclarationTreeNode parent) {
        super(fieldName, value, fieldType, parent);
    }

    public CollectionParameterTreeNode(ParameterWithValueDeclaration paramDescription, ParameterDeclarationTreeNode parent) {
        super(paramDescription, parent);
    }

    @Override
    public String getDisplayedValue() {
        StringBuilder builder = new StringBuilder();
        if(isLeaf()){
            builder.append("Empty ");
        }
        builder.append("Collection of ");
        builder.append(getType().getComponentClass().getDisplayName(INameSpacedThing.SHORT));
        return builder.toString();
    }

    @Override
    public String getNodeType() {
        return COLLECTION_TYPE;
    }

    @Override
    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildernMap() {
        if (isValueNull()) {
            return new LinkedHashMap<Object, ParameterDeclarationTreeNode>();
        } else {
            Iterator<Object> iterator = getType().getAggregateInfo().getIterator(getValue());
            IOpenClass collectionElementType = getType().getComponentClass();
            int index = 0;
            LinkedHashMap<Object, ParameterDeclarationTreeNode> elements = new LinkedHashMap<Object, ParameterDeclarationTreeNode>();
            while (iterator.hasNext()) {
                Object element = iterator.next();
                elements.put(index, TestTreeBuilder.createNode(collectionElementType, element, null, this));
                index++;
            }
            return elements;
        }
    }

    @Override
    protected Object constructValueInternal() {
        IAggregateInfo info = getType().getAggregateInfo();
        IOpenClass componentType = info.getComponentType(getType());
        int elementsCount = getChildernMap().size();
        Object ary = info.makeIndexedAggregate(componentType, new int[] { elementsCount });

        IOpenIndex index = info.getIndex(getType(), JavaOpenClass.INT);

        for (int i = 0; i < elementsCount; i++) {
            index.setValue(ary, new Integer(i), getChildernMap().get(i).getValueForced());
        }
        return ary;
    }
}
