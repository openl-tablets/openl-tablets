package org.openl.rules.webstudio.web.test;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.openl.base.INameSpacedThing;
import org.openl.rules.testmethod.ExecutionParamDescription;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaOpenClass;

public class CollectionFieldNode extends FieldDescriptionTreeNode {
    public static final String COLLECTION_TYPE = "collection";

    public CollectionFieldNode(String fieldName, Object value, IOpenClass fieldType, FieldDescriptionTreeNode parent) {
        super(fieldName, value, fieldType, parent);
    }

    public CollectionFieldNode(ExecutionParamDescription paramDescription, FieldDescriptionTreeNode parent) {
        super(paramDescription, parent);
    }

    @Override
    public String getDisplayedValue() {
        StringBuilder builder = new StringBuilder();
        if(isLeaf()){
            builder.append("Empty ");
        }
        builder.append("Collection of ");
        builder.append(getFieldType().getComponentClass().getDisplayName(INameSpacedThing.SHORT));
        return builder.toString();
    }

    @Override
    public String getNodeType() {
        return COLLECTION_TYPE;
    }

    @Override
    protected LinkedHashMap<Object, FieldDescriptionTreeNode> initChildernMap() {
        if (isValueNull()) {
            return new LinkedHashMap<Object, FieldDescriptionTreeNode>();
        } else {
            Iterator<Object> iterator = getFieldType().getAggregateInfo().getIterator(getValue());
            IOpenClass arrayElementType = getFieldType().getComponentClass();
            int index = 0;
            LinkedHashMap<Object, FieldDescriptionTreeNode> elements = new LinkedHashMap<Object, FieldDescriptionTreeNode>();
            while (iterator.hasNext()) {
                Object arrayElement = iterator.next();
                elements.put(index, TestTreeBuilder.createNode(arrayElementType, arrayElement, null, this));
                index++;
            }
            return elements;
        }
    }

    @Override
    protected Object constructValueInternal() {
        IAggregateInfo info = getFieldType().getAggregateInfo();
        IOpenClass componentType = info.getComponentType(getFieldType());
        int elementsCount = getChildernMap().size();
        Object ary = info.makeIndexedAggregate(componentType, new int[] { elementsCount });

        IOpenIndex index = info.getIndex(getFieldType(), JavaOpenClass.INT);

        for (int i = 0; i < elementsCount; i++) {
            index.setValue(ary, new Integer(i), getChildernMap().get(i).getValueForced());
        }
        return ary;
    }
}
