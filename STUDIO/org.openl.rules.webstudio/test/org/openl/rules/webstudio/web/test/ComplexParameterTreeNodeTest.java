package org.openl.rules.webstudio.web.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;
import org.openl.types.java.JavaOpenClass;

public class ComplexParameterTreeNodeTest {
    @Test
    public void testGetChildrenMap() {
        ComplexParameterTreeNode node = createNode(new My());
        
        LinkedHashMap<Object, ParameterDeclarationTreeNode> childrenMap = node.getChildrenMap();
        
        assertTrue(childrenMap.containsKey("name"));
        assertEquals(childrenMap.get("name").getValue(), "test");
        
        assertFalse(childrenMap.containsKey("value"));
    }
    
    @Test
    public void testThrowingMethod() {
        ComplexParameterTreeNode node = createNode(new ThrowingField());
        LinkedHashMap<Object, ParameterDeclarationTreeNode> childrenMap = node.getChildrenMap();
        
        assertTrue(childrenMap.containsKey("name"));
        assertEquals(childrenMap.get("name").getValue(), "test");

        assertTrue(childrenMap.containsKey("value"));
    }

    
    @Test(timeout = 1000)
    public void testCyclicReferences() {
        // Wee need to check a cyclic references, because JSF loads all child nodes before building a tree
        
        ComplexParameterTreeNode node = createNode(new SelfReference());
        assertTrue(node.getChildrenMap().containsKey("value"));
        
        // If there are a cyclic references, either StackOverflowError will be thrown or there will be a timeout
        assertFalse(getAllChildren(node).isEmpty());
        
        Container a = new Container();
        Container b = new Container();
        Container c = new Container();
        a.value = b;
        b.value = c;
        c.value = a;
        
        // If there are a cyclic references, either StackOverflowError will be thrown or there will be a timeout
        assertFalse(getAllChildren(createNode(a)).isEmpty());
    }

    private ComplexParameterTreeNode createNode(Object object) {
        JavaOpenClass openClass = JavaOpenClass.getOpenClass(object.getClass());
        return new ComplexParameterTreeNode(null, object, openClass, null, null, null);
    }
    
    private List<ParameterDeclarationTreeNode> getAllChildren(ParameterDeclarationTreeNode node) {
        // This method emulates a JSF's tree behavior
        
        Collection<ParameterDeclarationTreeNode> children = node.getChildrenMap().values();
        List<ParameterDeclarationTreeNode> values = new ArrayList<>(children);
        
        for (ParameterDeclarationTreeNode child : children) {
            values.addAll(getAllChildren(child));
        }
        
        return values;
    }
    
    //  Classes for test purposes

    public static class My {
        public String getName() {
            return "test";
        }
        public void setName(String name) {}
    }

    public static class ThrowingField {
        public String getName() {
            return "test";
        }
        public void setName(String name) {}

        public String getValue() {
            throw new UnsupportedOperationException();
        }

        public void setValue(String value) {}
    }
    
    public static class SelfReference {
        public SelfReference getValue() {
            return this;
        }

        public void setValue(SelfReference reference) { }
    }
    
    public static class Container {
        public Container value;
    }
}

