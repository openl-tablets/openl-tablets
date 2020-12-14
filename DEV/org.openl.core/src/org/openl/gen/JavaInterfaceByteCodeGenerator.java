package org.openl.gen;

import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.openl.gen.writers.AbstractMethodWriter;
import org.openl.gen.writers.ChainedBeanByteCodeWriter;

/**
 * Generates Java Interface
 *
 * @author Vladyslav Pikus
 */
public class JavaInterfaceByteCodeGenerator {

    static final String DEFAULT_PACKAGE = "org.openl.generated.";

    private final String nameWithPackage;
    private final ChainedBeanByteCodeWriter writerChain;

    /**
     * Initialize java interface generator with given parameters
     *
     * @param nameWithPackage interface java name with package
     * @param methods method descriptions to generate
     */
    JavaInterfaceByteCodeGenerator(String nameWithPackage, List<MethodDescription> methods) {
        this.nameWithPackage = nameWithPackage.replace('.', '/');
        if (methods != null) {
            ChainedBeanByteCodeWriter writerChain = null;
            for (MethodDescription description : methods) {
                writerChain = new AbstractMethodWriter(description, writerChain);
            }
            this.writerChain = writerChain;
        } else {
            this.writerChain = null;
        }
    }

    /**
     * Write Java Interface description
     *
     * @param cw class writer
     */
    private void visitInterfaceDescription(ClassWriter cw) {
        cw.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE,
                nameWithPackage,
                null,
                Object.class.getName().replace('.', '/'),
                null);
    }

    /**
     * Writes Java Interface
     *
     * @return resulted class writer
     */
    private ClassWriter writeInterface() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        visitInterfaceDescription(cw);
        if (writerChain != null) {
            writerChain.write(cw);
        }
        return cw;
    }

    /**
     * Get bytecode of generated Java Interface
     *
     * @return bytecode of generated Java Interface
     */
    public byte[] byteCode() {
        return writeInterface().toByteArray();
    }

}
