package org.openl.rules.webstudio.web.test;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaCollectionAggregateInfo;
import org.openl.types.java.JavaListAggregateInfo;
import org.openl.types.java.JavaMapAggregateInfo;
import org.openl.types.java.JavaOpenClass;
import org.richfaces.model.TreeNode;

public class CollectionParameterTreeNode extends ParameterDeclarationTreeNode {
    public static final String COLLECTION_TYPE = "collection";
    private final IOpenField previewField;
    private final boolean hasExplainLinks;

    public CollectionParameterTreeNode(String fieldName, Object value, IOpenClass fieldType, ParameterDeclarationTreeNode parent, IOpenField previewField, boolean hasExplainLinks) {
        super(fieldName, value, fieldType, parent);
        this.previewField = previewField;
        this.hasExplainLinks = hasExplainLinks;
    }

    @Override
    public String getDisplayedValue() {
    	return Utils.displayNameForCollection(getType(), isLeaf());
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

        IOpenIndex index = info.getIndex(getType());

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

    @Override
    public void addChild(Object elementNum, TreeNode element) {
        Object oldCollection = getValue();

        IOpenClass arrayType = getType();
        IAggregateInfo info = arrayType.getAggregateInfo();
        Object newCollection = info.makeIndexedAggregate(info.getComponentType(arrayType), new int[] { getChildren().size() + 1 });
        IOpenIndex index = info.getIndex(arrayType);

        int i = 0;
        for (ParameterDeclarationTreeNode node : getChildren()) {
            index.setValue(newCollection, getKeyFromElementNum(i), node.getValue());
            i++;
        }

        index.setValue(newCollection, getKeyFromElementNum(null), getEmptyValue());

        setValueForced(newCollection);
    }

    @Override
    public void removeChild(Object elementNum) {
        super.removeChild(elementNum);

        // Create new value based on changed child elements count
        IOpenClass arrayType = getType();
        IAggregateInfo info = arrayType.getAggregateInfo();
        Object newCollection = info.makeIndexedAggregate(arrayType.getComponentClass(), new int[] { getChildren().size()});
        IOpenIndex index = info.getIndex(arrayType);

        int i = 0;
        for (ParameterDeclarationTreeNode node : getChildren()) {
            index.setValue(newCollection, getKeyFromElementNum(i), node.getValue());
            i++;
        }

        setValueForced(newCollection);
    }

    protected Object getEmptyValue() {
        return null;
    }

    protected Object getKeyFromElementNum(Object elementNum) {
        if (elementNum == null) {
            return getChildren().size();
        }
        return elementNum;
    }
}
