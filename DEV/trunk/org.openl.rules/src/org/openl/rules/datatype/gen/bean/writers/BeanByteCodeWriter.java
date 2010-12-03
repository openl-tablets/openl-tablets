package org.openl.rules.datatype.gen.bean.writers;

import org.objectweb.asm.ClassWriter;

public interface BeanByteCodeWriter {
    
    void write(ClassWriter classWriter);
}
