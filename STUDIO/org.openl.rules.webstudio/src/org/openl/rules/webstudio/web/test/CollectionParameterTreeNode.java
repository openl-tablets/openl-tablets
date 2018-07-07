package org.openl.rules.webstudio.web.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenIndex;
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
            return new LinkedHashMap<>();
        } else {
            Iterator<Object> iterator = getType().getAggregateInfo().getIterator(getValue());
            IOpenClass collectionElementType = getType().getComponentClass();
            int index = 0;
            LinkedHashMap<Object, ParameterDeclarationTreeNode> elements = new LinkedHashMap<>();
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
        int elementsCount = getChildrenMap().size();
        Object ary = info.makeIndexedAggregate(componentType, elementsCount);

        IOpenIndex index = info.getIndex(getType());

        for (int i = 0; i < elementsCount; i++) {
            ParameterDeclarationTreeNode node = getChildrenMap().get(i);
            node.getValueForced();
            Object key = getKeyFromElementNum(i);
            if (key != null) {
                index.setValue(ary, key, getNodeValue(node));
            }
        }
        return ary;
    }

    @Override
    public void addChild(Object elementNum, TreeNode element) {
        int nextChildNum = getChildren().size();
        Object value = element == null ? null : ((ParameterDeclarationTreeNode) element).getValue();
        ParameterDeclarationTreeNode node = createNode(null, value);
        if (nextChildNum > 0) {
            initComplexNode(getChild(nextChildNum - 1), node);
        }
        super.addChild(nextChildNum, node);
        saveChildNodesToValue();
    }

    protected void initComplexNode(ParameterDeclarationTreeNode from, ParameterDeclarationTreeNode to) {
        if (!(to instanceof ComplexParameterTreeNode)) {
            return;
        }
        ComplexParameterTreeNode complexNode = (ComplexParameterTreeNode) to;
        IOpenClass type = from.getType();
        if (from instanceof ComplexParameterTreeNode) {
            IOpenClass typeToCreate = ((ComplexParameterTreeNode) from).getTypeToCreate();
            if (typeToCreate != null) {
                type = typeToCreate;
            }
        }
        complexNode.setTypeToCreate(type);
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
        // Children keys in children map must be remapped because element in the middle was deleted
        updateChildrenKeys();
    }

    protected void updateChildrenKeys() {
        LinkedHashMap<Object, ParameterDeclarationTreeNode> elements = getChildrenMap();
        // Values in LinkedHashMap are in the same order as they were inserted before
        List<ParameterDeclarationTreeNode> values = new ArrayList<>(elements.values());
        // Reinsert values with new keys
        elements.clear();
        for (int index = 0; index < values.size(); index++) {
            elements.put(index, values.get(index));
        }
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
        Object newCollection = info.makeIndexedAggregate(arrayType.getComponentClass(), getChildren().size());
        IOpenIndex index = info.getIndex(arrayType);

        int i = 0;
        for (ParameterDeclarationTreeNode node : getChildren()) {
            Object key = getKeyFromElementNum(i);
            if (key != null) {
                index.setValue(newCollection, key, getNodeValue(node));
            }
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
