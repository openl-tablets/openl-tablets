package org.openl.rules.datatype.gen.bean.writers;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.openl.rules.datatype.gen.FieldDescription;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class DefaultBeanByteCodeWriterTest {

    @Test
    public void testNullParentClass() {
        DefaultBeanByteCodeWriter testedWriter = createStubInstance(null);
        assertEquals("java/lang/Object", testedWriter.getParentInternalName());
    }

    @Test
    public void testObjectParentClass() {
        DefaultBeanByteCodeWriter testedWriter = createStubInstance(Object.class);
        assertEquals("java/lang/Object", testedWriter.getParentInternalName());
    }

    @Test
    public void testStringParentClass() {
        DefaultBeanByteCodeWriter testedWriter = createStubInstance(String.class);
        assertEquals("java/lang/String", testedWriter.getParentInternalName());
    }

    private DefaultBeanByteCodeWriter createStubInstance(Class<?> parentClass) {
        return new DefaultBeanByteCodeWriter(null, parentClass, Collections.<String, FieldDescription>emptyMap()) {
            @Override
            public void write(ClassWriter classWriter) {
                // No implementation.
            }
        };
    }
}
