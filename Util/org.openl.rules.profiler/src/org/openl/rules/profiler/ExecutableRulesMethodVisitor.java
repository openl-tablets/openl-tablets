package org.openl.rules.profiler;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ExecutableRulesMethodVisitor extends ClassVisitor {

    /**
     * the return opcode for different type
     */
    protected static final Map<Type, Integer> RETURN_OPCODES = new HashMap<>();
    /**
     * the load opcode for different type
     */
    protected static final Map<Type, Integer> LOAD_OPCODES = new HashMap<>();

    static {
        RETURN_OPCODES.put(Type.VOID_TYPE, Opcodes.RETURN);
        RETURN_OPCODES.put(Type.BOOLEAN_TYPE, Opcodes.IRETURN);
        RETURN_OPCODES.put(Type.BYTE_TYPE, Opcodes.IRETURN);
        RETURN_OPCODES.put(Type.SHORT_TYPE, Opcodes.IRETURN);
        RETURN_OPCODES.put(Type.INT_TYPE, Opcodes.IRETURN);
        RETURN_OPCODES.put(Type.LONG_TYPE, Opcodes.LRETURN);
        RETURN_OPCODES.put(Type.FLOAT_TYPE, Opcodes.FRETURN);
        RETURN_OPCODES.put(Type.DOUBLE_TYPE, Opcodes.DRETURN);

        LOAD_OPCODES.put(Type.BOOLEAN_TYPE, Opcodes.ILOAD);
        LOAD_OPCODES.put(Type.BYTE_TYPE, Opcodes.ILOAD);
        LOAD_OPCODES.put(Type.SHORT_TYPE, Opcodes.ILOAD);
        LOAD_OPCODES.put(Type.INT_TYPE, Opcodes.ILOAD);
        LOAD_OPCODES.put(Type.LONG_TYPE, Opcodes.LLOAD);
        LOAD_OPCODES.put(Type.FLOAT_TYPE, Opcodes.FLOAD);
        LOAD_OPCODES.put(Type.DOUBLE_TYPE, Opcodes.DLOAD);
    }

    private Class<?> superClass;
    private ClassVisitor cv;

    public ExecutableRulesMethodVisitor(int api, ClassVisitor classVisitor, Class<?> superClass) {
        super(api);
        this.cv = classVisitor;
        this.superClass = Objects.requireNonNull(superClass);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor av = cv.visitAnnotation(descriptor, visible);
        av.visitEnd();
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions) {
        if (Modifier.isStatic(access) || Modifier.isPrivate(access))
            return null;
        if ("<init>".equals(name) || Modifier.isProtected(access))
            access = Opcodes.ACC_PUBLIC;

        Type methodType = Type.getMethodType(descriptor);
        Type[] argumentTypes = methodType.getArgumentTypes();

        if ("<init>".equals(name)) {
            MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);

            // load this
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            // load arguments
            for (int i = 0; i < argumentTypes.length; i++) {
                mv.visitVarInsn(LOAD_OPCODES.getOrDefault(argumentTypes[i], Opcodes.ALOAD), i + 1);
            }

            // invoke super.{method}()
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(superClass), name, descriptor, false);

            // handle return
            mv.visitInsn(RETURN_OPCODES.getOrDefault(methodType.getReturnType(), Opcodes.ALOAD));
            // for ClassWriter.COMPUTE_FRAMES the max*s not required correct
            int maxLocals = argumentTypes.length + 1;
            mv.visitMaxs(maxLocals + 2, maxLocals);
            mv.visitEnd();
        }
        if ("innerInvoke".equals(name)) {
            MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
            // load this
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            // load arguments
            for (int i = 0; i < argumentTypes.length; i++) {
                mv.visitVarInsn(LOAD_OPCODES.getOrDefault(argumentTypes[i], Opcodes.ALOAD), i + 1);
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(superClass), name, descriptor, false);

            // handle return
            mv.visitInsn(Opcodes.ARETURN);

            // for ClassWriter.COMPUTE_FRAMES the max*s not required correct
            int maxLocals = argumentTypes.length + 1;
            mv.visitMaxs(maxLocals + 2, maxLocals);
            mv.visitEnd();
        }
        return null;
    }
}
