package org.openl.gen;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

/**
 * Generates byte code implementation for target Interface type.
 *
 * @author Vladyslav Pikus
 */
class JavaInterfaceImplGenerator extends POJOByteCodeGenerator {

    /**
     * A getter or a setter of an interface that reads or writes the value of a property of the bean. A getter takes
     * no parameter.
     *
     * @param method    the method as the interface declares it
     * @param property  the property it reads or writes
     * @param supported {@code false} when the type of the method does not fit the value: the method throws
     *                  {@link UnsupportedOperationException}
     */
    record Accessor(MethodDescription method, String property, boolean supported) {
    }

    private final Class<?> clazzInterface;
    private final List<MethodDescription> beanStubMethods;
    private final List<Accessor> accessors;

    JavaInterfaceImplGenerator(String beanName,
                               Class<?> clazzInterface,
                               Map<String, FieldDescription> beanFields,
                               List<MethodDescription> beanStubMethods,
                               List<Accessor> accessors) {

        super(beanName,
                beanFields,
                POJOByteCodeGenerator.OBJECT_TYPE_DESCRIPTION,
                Collections.emptyMap(),
                false,
                true,
                false);
        this.clazzInterface = clazzInterface;
        this.beanStubMethods = beanStubMethods;
        this.accessors = accessors;
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
        for (Accessor accessor : accessors) {
            visitAccessor(classWriter, accessor);
        }
    }

    /**
     * Writes a getter or a setter of the interface that reads or writes the value of its property, such as
     * {@code isActive()}, or {@code Object getValue()} and {@code setValue(Object)} for a property of the type
     * {@code String}. The value is converted to the type of the method: cast, boxed or unboxed, an absent value
     * unboxing to the default value of the primitive.
     */
    private void visitAccessor(ClassWriter classWriter, Accessor accessor) {
        var method = accessor.method();
        var owner = Type.getType(getBeanNameDescriptor());
        var fieldType = Type.getType(getFields().get(accessor.property()).getTypeDescriptor());
        var generator = new GeneratorAdapter(Opcodes.ACC_PUBLIC,
                new Method(method.getName(), buildMethodDescriptor(method)),
                null,
                null,
                classWriter);
        if (!accessor.supported()) {
            generator.throwException(Type.getType(UnsupportedOperationException.class),
                    "The property '%s' is kept as '%s'.".formatted(accessor.property(), fieldType.getClassName()));
        } else if (generator.getArgumentTypes().length == 0) {
            generator.loadThis();
            generator.getField(owner, accessor.property(), fieldType);
            convert(generator, fieldType, generator.getReturnType());
            generator.returnValue();
        } else {
            generator.loadThis();
            generator.loadArg(0);
            convert(generator, generator.getArgumentTypes()[0], fieldType);
            generator.putField(owner, accessor.property(), fieldType);
            generator.returnValue();
        }
        generator.endMethod();
    }

    /** Converts the value on the stack from one type to the other one. */
    private static void convert(GeneratorAdapter generator, Type from, Type to) {
        if (from.equals(to)) {
            return;
        }
        if (isPrimitive(from)) {
            // the wrapper of the primitive is a subtype of the reference type
            generator.valueOf(from);
        } else if (isPrimitive(to)) {
            // an absent value is the default value of the primitive
            visitDefaultTypeValue(generator, to.getClassName());
            generator.valueOf(to);
            generator.invokeStatic(Type.getType(Objects.class),
                    Method.getMethod("Object requireNonNullElse(Object, Object)"));
            generator.unbox(to);
        } else {
            generator.checkCast(to);
        }
    }

    private static boolean isPrimitive(Type type) {
        return type.getSort() < Type.ARRAY;
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
    private static void visitDefaultTypeValue(MethodVisitor methodVisitor, String type) {
        switch (type) {
            case "void":
                break;
            case "byte", "short", "int", "char", "boolean":
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
