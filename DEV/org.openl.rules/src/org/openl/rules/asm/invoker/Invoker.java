package org.openl.rules.asm.invoker;

import org.objectweb.asm.MethodVisitor;

/**
 * An abstraction of invoker of MethodVisitor.
 * 
 * @author Yury Molchan
 */
public interface Invoker {

    /**
     * Calls a visit*() method in a MethodVisitor.
     * 
     * @param methodVisitor a method visitor
     */
    void invoke(MethodVisitor methodVisitor);
}
