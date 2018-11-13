package org.openl.rules.webstudio.web.test;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.richfaces.model.TreeNode;

public abstract class ParameterDeclarationTreeNode extends ParameterWithValueDeclaration implements TreeNode {

    private ParameterDeclarationTreeNode parent;
    private LinkedHashMap<Object, ParameterDeclarationTreeNode> children;

    public ParameterDeclarationTreeNode(String fieldName,
            Object value,
            IOpenClass fieldType,
            ParameterDeclarationTreeNode parent,
            IOpenField keyField) {
        super(fieldName, value, fieldType, keyField);
        this.parent = parent;
    }

    public ParameterDeclarationTreeNode(String fieldName,
            Object value,
            IOpenClass fieldType,
            ParameterDeclarationTreeNode parent) {
        this(fieldName, value, fieldType, parent, null);
    }

    public ParameterDeclarationTreeNode getParent() {
        return parent;
    }

    @Override
    public boolean isLeaf() {
        return getChildrenMap().isEmpty();
    }

    public abstract String getDisplayedValue();

    public boolean isValueNull() {
        return getValue() == null;
    }

    public boolean isElementOfCollection() {
        return parent instanceof CollectionParameterTreeNode;
    }

    public void setValueForced(Object value) {
        setValue(value);
        reset();
    }
    
    public Object getValueForced(){
        if(isValueNull()){
            return null;
        }else{
            return constructValueInternal();
        }
    }
    
    protected abstract Object constructValueInternal();

    public abstract String getNodeType();

    public String getTreeText() {
        StringBuilder buff = new StringBuilder();
        if (getName() != null) {
            buff.append(getName());
            buff.append(" = ");
        }
        if (isValueNull()) {
            buff.append("null");
        } else {
            buff.append(getDisplayedValue());
        }
        return buff.toString();
    }
    
    public void reset(){
        children = null;
    }

    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> getChildrenMap(){
        if(children == null){
            children = initChildrenMap();
        }
        return children;
    }

    protected abstract LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildrenMap();
    
    @Override
    public void addChild(Object key, TreeNode node) {
        if(node instanceof ParameterDeclarationTreeNode){
            getChildrenMap().put(key, (ParameterDeclarationTreeNode)node);
        }
    }
    
    @Override
    public ParameterDeclarationTreeNode getChild(Object key) {
        return getChildrenMap().get(key);
    }
    
    public Collection<ParameterDeclarationTreeNode> getChildren(){
        return getChildrenMap().values();
    }
    
    @Override
    public Iterator<Object> getChildrenKeysIterator() {
        return getChildrenMap().keySet().iterator();
    }

    @Override
    public int indexOf(Object key) {
        Iterator<Object> keysIterator = getChildrenKeysIterator();
        int i = 0;
        while(keysIterator.hasNext()){
            if(keysIterator.next() == key){
                return i;
            }else{
                i++;
            }
        }
        return -1;
    }

    @Override
    public void insertChild(int index, Object key, TreeNode node) {
        addChild(key, node);
    }

    @Override
    public void removeChild(Object key) {
        getChildrenMap().remove(key);
    }

    public void replaceChild(ParameterDeclarationTreeNode oldNode, ParameterDeclarationTreeNode newNode) {
        LinkedHashMap<Object, ParameterDeclarationTreeNode> childrenMap = getChildrenMap();
        for (Map.Entry<Object, ParameterDeclarationTreeNode> entry : childrenMap.entrySet()) {
            if (entry.getValue() == oldNode) {
                entry.setValue(newNode);
                break;
            }
        }
    }
}