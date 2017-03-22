package org.openl.rules.webstudio.web.test;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenIndex;
import org.openl.types.java.*;

public class CollectionParameterTreeNode extends ParameterDeclarationTreeNode {
    public static final String COLLECTION_TYPE = "collection";
    private final IOpenField previewField;
    private final boolean hasExplainLinks;

    public CollectionParameterTreeNode(String fieldName, Object value, IOpenClass fieldType, ParameterDeclarationTreeNode parent, IOpenField previewField, boolean hasExplainLinks) {
        super(fieldName, value, fieldType, parent);
        this.previewField = previewField;
        this.hasExplainLinks = hasExplainLinks;
    }

    public CollectionParameterTreeNode(ParameterWithValueDeclaration paramDescription, ParameterDeclarationTreeNode parent) {
        super(paramDescription, parent);
        previewField = null;
        hasExplainLinks = true;
    }

    @Override
    public String getDisplayedValue() {
    	return OpenClassHelper.displayNameForCollection(getType(), isLeaf());        
    }

    @Override
    public String getNodeType() {
        return COLLECTION_TYPE;
    }

    @Override
    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildrenMap() {
        if (isValueNull()) {
            return new LinkedHashMap<Object, ParameterDeclarationTreeNode>();
        } else {
            Iterator<Object> iterator = getType().getAggregateInfo().getIterator(getValue());
            IOpenClass collectionElementType = getType().getComponentClass();
            int index = 0;
            LinkedHashMap<Object, ParameterDeclarationTreeNode> elements = new LinkedHashMap<Object, ParameterDeclarationTreeNode>();
            while (iterator.hasNext()) {
                Object element = iterator.next();
                IOpenClass type = collectionElementType;
                if (type == JavaOpenClass.OBJECT && element != null) {
                    // Show content of complex objects
                    type = JavaOpenClass.getOpenClass(element.getClass());
                }
                elements.put(index, ParameterTreeBuilder.createNode(type, element, previewField, null, this, hasExplainLinks));
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
            index.setValue(ary, i, getChildernMap().get(i).getValueForced());
        }
        return ary;
    }

    public boolean isJavaCollection() {
        IAggregateInfo aggregateInfo = getType().getAggregateInfo();
        return aggregateInfo instanceof JavaListAggregateInfo
                || aggregateInfo instanceof JavaCollectionAggregateInfo
                || aggregateInfo instanceof JavaMapAggregateInfo;
    }
}
