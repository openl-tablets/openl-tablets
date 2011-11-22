package org.openl.rules.webstudio.web.test;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.openl.rules.testmethod.ExecutionParamDescription;
import org.openl.types.IOpenClass;
import org.richfaces.model.TreeNode;

public abstract class FieldDescriptionTreeNode implements TreeNode {
    private IOpenClass fieldType;
    private String fieldName;
    private Object value;

    private FieldDescriptionTreeNode parent;
    private LinkedHashMap<Object, FieldDescriptionTreeNode> children;

    public FieldDescriptionTreeNode(String fieldName,
            Object value,
            IOpenClass fieldType,
            FieldDescriptionTreeNode parent) {
        this.fieldName = fieldName;
        this.value = value;
        this.fieldType = fieldType;
        this.parent = parent;
    }

    public FieldDescriptionTreeNode(ExecutionParamDescription paramDescription, FieldDescriptionTreeNode parent) {
        this(paramDescription.getParamName(), paramDescription.getValue(), paramDescription.getParamType(), parent);
    }

    public FieldDescriptionTreeNode getParent() {
        return parent;
    }

    @Override
    public boolean isLeaf() {
        return getChildernMap().isEmpty();
    }

    public IOpenClass getFieldType() {
        return fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

    protected Object getValue() {
        return value;
    }

    public abstract String getDisplayedValue();

    public boolean isValueNull() {
        return value == null;
    }

    public boolean isElementOfCollection() {
        return parent instanceof CollectionFieldNode;
    }

    public void setValueForced(Object value) {
        this.value = value;
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
        if (getFieldName() != null) {
            buff.append(getFieldName());
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

    protected LinkedHashMap<Object, FieldDescriptionTreeNode> getChildernMap(){
        if(children == null){
            children = initChildernMap();
        }
        return children;
    }

    protected abstract LinkedHashMap<Object, FieldDescriptionTreeNode> initChildernMap();
    
    @Override
    public void addChild(Object key, TreeNode node) {
        if(node instanceof FieldDescriptionTreeNode){
            getChildernMap().put(key, (FieldDescriptionTreeNode)node);
        }
    }
    
    @Override
    public FieldDescriptionTreeNode getChild(Object key) {
        return getChildernMap().get(key);
    }
    
    public Collection<FieldDescriptionTreeNode> getChildren(){
        return getChildernMap().values();
    }
    
    @Override
    public Iterator<Object> getChildrenKeysIterator() {
        return getChildernMap().keySet().iterator();
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
        getChildernMap().remove(key);
    }

}