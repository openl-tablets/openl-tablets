package org.openl.gen;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Generates byte code implementation for target Interface type.
 *
 * @author Vladyslav Pikus
 */
class JavaInterfaceImplGenerator extends POJOByteCodeGenerator {

    private final Class<?> clazzInterface;
    private final List<MethodDescription> beanStubMethods;

    JavaInterfaceImplGenerator(String beanName,
            Class<?> clazzInterface,
            Map<String, FieldDescription> beanFields,
            List<MethodDescription> beanStubMethods) {

        super(beanName,
            beanFields,
            POJOByteCodeGenerator.OBJECT_TYPE_DESCRIPTION,
            Collections.emptyMap(),
            false,
            true,
            false);
        this.clazzInterface = clazzInterface;
        this.beanStubMethods = beanStubMethods;
    }

    /**
     * Generates method stubs <br/>
     * If method has return type as any object, generated stub will return NULL value<br/>
     * If method has return type as any primitive, generated stub will return default values for them<br/>
     *
     * @param classWriter
     */
    @Override
    protected void visitExtraByteCodeGeneration(ClassWriter classWriter) {
        for (MethodDescription method : beanStubMethods) {
            MethodVisitor methodVisitor = classWriter
                .visitMethod(Opcodes.ACC_PUBLIC, method.getName(), buildMethodDescriptor(method), null, null);

            methodVisitor.visitCode();
            visitDefaultTypeValue(methodVisitor, method.getReturnType().getTypeName());
            String retClass = method.getReturnType().getTypeDescriptor();
            Type type = Type.getType(retClass);
            methodVisitor.visitInsn(type.getOpcode(Opcodes.IRETURN));
            methodVisitor.visitMaxs(0, 0);
            methodVisitor.visitEnd();
        }
    }

    /**
     * Push default value for target type to the stack: <br/>
     * <p>
     * byte, short, int, char - 0 <br/>
     * boolean - false <br/>
     * long - 0L <br/>
     * float - 0F <br/>
     * double - 0D <br/>
     * Object - null <br/>
     * </p>
     *
     * @param methodVisitor
     * @param type
     */
    private void visitDefaultTypeValue(MethodVisitor methodVisitor, String type) {
        switch (type) {
            case "void":
                break;
            case "byte":
            case "short":
            case "int":
            case "char":
            case "boolean":
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                break;
            case "long":
                methodVisitor.visitInsn(Opcodes.LCONST_0);
                break;
            case "float":
                methodVisitor.visitInsn(Opcodes.FCONST_0);
                break;
            case "double":
                methodVisitor.visitInsn(Opcodes.DCONST_0);
                break;
            default:
                methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        }
    }

    private String buildMethodDescriptor(MethodDescription method) {
        StringBuilder builder = new StringBuilder("(");
        for (TypeDescription arg : method.getArgsTypes()) {
            builder.append(arg.getTypeDescriptor());
        }
        builder.append(')').append(method.getReturnType().getTypeDescriptor());
        return builder.toString();
    }

    @Override
    protected String[] getDefaultInterfaces() {
        String[] defaultInterfaces = super.getDefaultInterfaces();
        String[] res = new String[defaultInterfaces.length + 1];

        res[0] = Type.getInternalName(clazzInterface);
        System.arraycopy(defaultInterfaces, 0, res, 1, res.length - 1);
        return res;
    }

}
