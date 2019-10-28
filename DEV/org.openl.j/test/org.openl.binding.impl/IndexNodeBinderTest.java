package org.openl.binding.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.INodeBinder;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.UnaryNode;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

@RunWith(Parameterized.class)
public class IndexNodeBinderTest {
    private static final String DATATYPE_CLASS = "CustomDatatype";
    public Class clazz;

    @Parameterized.Parameters(name = "Run {index}: class={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { java.lang.String.class }, { int.class } });
    }

    public IndexNodeBinderTest(Class clazz) {
        this.clazz = clazz;
    }

    @Test
    public void bindTarget_error_indexOperatorIsNotFound() throws Exception {
        // mock objects
        UnaryNode parentNode = mock(UnaryNode.class);
        when(parentNode.getNumberOfChildren()).thenReturn(1);
        ISyntaxNode childNode = mock(ISyntaxNode.class);
        when(parentNode.getChild(0)).thenReturn(childNode);
        IBindingContext bindingContext = mock(IBindingContext.class);
        INodeBinder childBinder = mock(INodeBinder.class);
        IBoundNode boundNode = mock(IBoundNode.class);
        JavaOpenClass childClass = new JavaOpenClass(clazz);
        when(boundNode.getType()).thenReturn(childClass);
        when(childBinder.bind(childNode, bindingContext)).thenReturn(boundNode);
        when(bindingContext.findBinder(childNode)).thenReturn(childBinder);
        IBoundNode targetNode = mock(IBoundNode.class);
        // create custom data type
        CustomDatatypeOpenClass targetClass = new CustomDatatypeOpenClass(DATATYPE_CLASS, null);
        when(targetNode.getType()).thenReturn(targetClass);
        // Index operation
        IndexNodeBinder binder = new IndexNodeBinder();
        binder.bindTarget(parentNode, bindingContext, targetNode);
        // catch error message
        ArgumentCaptor<SyntaxNodeException> captor = ArgumentCaptor.forClass(SyntaxNodeException.class);
        Mockito.verify(bindingContext).addError(captor.capture());
        SyntaxNodeException nodeException = captor.getValue();
        String expectedResult = String.format("Index operator %s[%s] is not found.", DATATYPE_CLASS, clazz.getName());
        assertEquals(expectedResult, nodeException.getMessage());
    }

    // emulate DatatypeOpenClass to avoid circular dependency on org.openl.rules
    private class CustomDatatypeOpenClass extends ADynamicClass {
        private final String javaName;
        private final String packageName;

        CustomDatatypeOpenClass(String javaName, String packageName) {
            super(javaName, null);
            this.javaName = javaName;
            this.packageName = packageName;
        }

        public String getJavaName() {
            return javaName;
        }

        public String getPackageName() {
            return packageName;
        }

        @Override
        public Object newInstance(IRuntimeEnv env) {
            return null;
        }
    }
}
