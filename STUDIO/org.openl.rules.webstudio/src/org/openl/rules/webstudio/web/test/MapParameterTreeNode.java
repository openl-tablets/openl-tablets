package org.openl.rules.webstudio.web.test;

import java.util.*;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.richfaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapParameterTreeNode extends CollectionParameterTreeNode {
    private final Logger log = LoggerFactory.getLogger(MapParameterTreeNode.class);

    public MapParameterTreeNode(String fieldName,
            Object value,
            IOpenClass fieldType,
            ParameterDeclarationTreeNode parent, IOpenField previewField, boolean hasExplainLinks) {
        super(fieldName, value, fieldType, parent, previewField, hasExplainLinks);
    }

    @Override
    protected Object getKeyFromElementNum(int elementNum) {
        if (elementNum >= getChildren().size()) {
            return null;
        }
        ParameterDeclarationTreeNode key = getChild(elementNum).getChild("key");
        if (key == null) {
            return null;
        }
        return key.getValue();
    }

    @Override
    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildrenMap() {
        if (isValueNull()) {
            return new LinkedHashMap<>();
        } else {
            IOpenClass collectionElementType = getType().getComponentClass();
            LinkedHashMap<Object, ParameterDeclarationTreeNode> elements = new LinkedHashMap<>();
            Map<Object, Object> map = getMap();
            int i = 0;
            try {
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    elements.put(i, createNode(entry.getKey(), entry.getValue()));
                    i++;
                }
            } catch (Exception e) {
                // Can throw UnsupportedOperationException for example.
                log.debug(e.getMessage(), e);
            }
            return elements;
        }
    }

    @Override
    public void addChild(Object elementNum, TreeNode element) {
        Object value = element == null ? null : ((ParameterDeclarationTreeNode) element).getValue();
        LinkedHashMap<Object, ParameterDeclarationTreeNode> childrenMap = getChildrenMap();
        int nextChildNum = childrenMap.size();

        ParameterDeclarationTreeNode node = createNode(null, value);
        ListIterator<ParameterDeclarationTreeNode> iterator = new ArrayList<>(childrenMap.values()).listIterator(nextChildNum);
        if (iterator.hasPrevious()) {
            ParameterDeclarationTreeNode lastChild = iterator.previous();
            ParameterDeclarationTreeNode lastKey = lastChild.getChild("key");
            ParameterDeclarationTreeNode lastValue = lastChild.getChild("value");
            while ((lastKey == null || lastValue == null) && iterator.hasPrevious()) {
                lastChild = iterator.previous();
                lastKey = lastChild.getChild("key");
                lastValue = lastChild.getChild("value");
            }

            if (lastKey != null) {
                initComplexNode(lastKey, node.getChild("key"));
            }
            if (lastValue != null) {
                initComplexNode(lastValue, node.getChild("value"));
            }
        }

        childrenMap.put(nextChildNum, node);
    }

    @Override
    public void removeChild(ParameterDeclarationTreeNode toDelete) {
        for (Iterator<ParameterDeclarationTreeNode> iterator = getChildren().iterator(); iterator.hasNext(); ) {
            ParameterDeclarationTreeNode node = iterator.next();
            if (node == toDelete) {
                Entry value = (Entry) node.getValue();
                if (value != null) {
                    getMap().remove(value.getKey());
                }
                iterator.remove();
                break;
            }
        }

        updateChildrenKeys();
    }

    @Override
    protected ParameterDeclarationTreeNode createNode(Object key, Object value) {
        Entry element = new Entry(getMap(), key, value);
        return ParameterTreeBuilder.createNode(JavaOpenClass.getOpenClass(element.getClass()), element, previewField, null, this, hasExplainLinks);
    }

    @Override
    protected Object getNodeValue(ParameterDeclarationTreeNode node) {
        return ((Entry) node.getValue()).getValue();
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> getMap() {
        return (Map<Object, Object>) getValue();
    }

    @RestrictDispose
    public static class Entry {
        private final Map<Object, Object> map;
        private Object key;
        private Object value;

        // Dummy constructor for UI
        public Entry() {
            map = new HashMap<>();
        }

        public Entry(Map<Object, Object> map, Object key, Object value) {
            this.map = map;
            this.key = key;
            this.value = value;
        }

        public Object getKey() {
            return key;
        }

        public void setKey(Object key) {
            // Remap value for old key to the new one
            Object value = this.key == null ? null : map.remove(this.key);
            if (value != null) {
                map.put(key, value);
            }

            this.key = key;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;

            if (key != null) {
                map.put(key, value);
            }
        }
    }
}
