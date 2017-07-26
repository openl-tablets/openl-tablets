package org.openl.rules.asm.invoker;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.binding.MethodUtil;

import java.lang.reflect.Method;

/**
 * An invoker of virtual methods.
 * 
 * @author Yury Molchan
 */
class VirtialInvoker implements Invoker {
    private static final Class<?>[] NO_TYPES = new Class<?>[] {};
    private final String owner;
    private final String methodName;
    private final String signature;

    private VirtialInvoker(String owner, String methodName, String signature) {
        this.owner = owner;
        this.methodName = methodName;
        this.signature = signature;
    }

    @Override
    public void invoke(MethodVisitor methodVisitor) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, methodName, signature);
    }

    static Invoker create(Class<?> methodOwner, String methodName) {
        return create(methodOwner, methodName, NO_TYPES);
    }

    static Invoker create(Class<?> methodOwner, String methodName, Class<?>[] paramTypes) {
        Method matchingMethod = MethodUtil.getMatchingAccessibleMethod(methodOwner, methodName, paramTypes);
        String signature = Type.getMethodDescriptor(matchingMethod);
        final String owner = Type.getInternalName(methodOwner);
        return new VirtialInvoker(owner, methodName, signature);
    }
}
