package org.openl.binding.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.NullOpenClass;

public class OrderByIndexNodeBinderTest {

    @Test
    public void createBoundNode_throws_indexOperatorIsNotFound() {
        // mock objects
        IBoundNode expressionNode = mock(IBoundNode.class);
        when(expressionNode.getType()).thenReturn(mock(NullOpenClass.class));
        IBindingContext bindingContext = mock(IBindingContext.class);
        // OrderBy operation
        OrderByIndexNodeBinder binder = new OrderByIndexNodeBinder();
        binder.createBoundNode(null, null, expressionNode, null, bindingContext);
        // catch error message
        ArgumentCaptor<SyntaxNodeException> captor = ArgumentCaptor.forClass(SyntaxNodeException.class);
        Mockito.verify(bindingContext).addError(captor.capture());
        SyntaxNodeException nodeException = captor.getValue();
        assertEquals("Order By expression requires typed parameter", nodeException.getMessage());
    }
}
