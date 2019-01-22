package org.openl.gen.writers;

import org.objectweb.asm.ClassWriter;

/**
 * Common interface for byte code writers.
 * 
 * @author DLiauchuk
 *
 */
public interface BeanByteCodeWriter {
    
    void write(ClassWriter classWriter);
}
