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
    protected final IOpenField previewField;
    protected final boolean hasExplainLinks;

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
            ParameterDeclarationTreeNode node = getChildernMap().get(i);
            node.getValueForced();
            index.setValue(ary, getKeyFromElementNum(i), getNodeValue(node));
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
        int nextChildNum = getChildren().size();
        Object value = element == null ? null : ((ParameterDeclarationTreeNode) element).getValue();
        super.addChild(nextChildNum, createNode(null, value));
        saveChildNodesToValue();
    }

    public void removeChild(ParameterDeclarationTreeNode toDelete) {
        int i = 0;
        for (ParameterDeclarationTreeNode node : getChildren()) {
            if (node == toDelete) {
                super.removeChild(i);
                break;
            }
            i++;
        }

        // Create new value based on changed child elements count
        saveChildNodesToValue();
        // Children keys in the map must be changed because element in the middle was deleted
        reset();
    }

    @Override
    public void replaceChild(ParameterDeclarationTreeNode oldNode, ParameterDeclarationTreeNode newNode) {
        super.replaceChild(oldNode, newNode);
        saveChildNodesToValue();
    }

    protected ParameterDeclarationTreeNode createNode(Object key, Object value) {
        return ParameterTreeBuilder.createNode(getType().getComponentClass(), value, previewField, null, this, hasExplainLinks);
    }

    private void saveChildNodesToValue() {
        IOpenClass arrayType = getType();
        IAggregateInfo info = arrayType.getAggregateInfo();
        Object newCollection = info.makeIndexedAggregate(arrayType.getComponentClass(), new int[] { getChildren().size()});
        IOpenIndex index = info.getIndex(arrayType);

        int i = 0;
        for (ParameterDeclarationTreeNode node : getChildren()) {
            index.setValue(newCollection, getKeyFromElementNum(i), getNodeValue(node));
            i++;
        }

        setValue(newCollection);
    }

    protected Object getKeyFromElementNum(int elementNum) {
        if (elementNum >= getChildren().size()) {
            return getChildren().size();
        }
        return elementNum;
    }

    protected Object getNodeValue(ParameterDeclarationTreeNode node) {
        return node.getValue();
    }
}
