package org.openl.rules.calc.element;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.sf.cglib.core.ReflectUtils;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.calc.ASpreadsheetField;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetRangeField extends ASpreadsheetField {

    private static final String GENERATED_CLASS_NAME_PREFIX = SpreadsheetRangeObject.class.getName() + "$";

    private final SpreadsheetCellField fstart;

    public SpreadsheetCellField getStart() {
        return fstart;
    }

    public SpreadsheetCellField getEnd() {
        return fend;
    }

    private final SpreadsheetCellField fend;

    private final Class<?> typeClass;

    private final IOpenCast[][] casts;

    public SpreadsheetRangeField(String name,
            SpreadsheetCellField fstart,
            SpreadsheetCellField fend,
            IOpenClass rangeType,
            IOpenCast[][] casts) {
        super(fstart.getDeclaringClass(), name, JavaOpenClass.getOpenClass(SpreadsheetRangeObject.class));
        this.fstart = fstart;
        this.fend = fend;
        this.casts = casts;
        try {
            Class<?> toClass = rangeType.getInstanceClass();
            typeClass = generateClass(GENERATED_CLASS_NAME_PREFIX + toClass.getName().replaceAll("\\.", "\\$"),
                toClass,
                Thread.currentThread().getContextClassLoader());
            setType(JavaOpenClass.getOpenClass(typeClass));
        } catch (Exception e) {
            throw new OpenlNotCheckedException(e);
        }
    }

    private static Class<?> generateClass(String className, Class<?> toClass, ClassLoader classLoader) throws Exception {
        try {
            Class<?> clazz = classLoader.loadClass(className);
            if (clazz != null) {
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            // Try to generate this class;
        }
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;
        cw.visit(Opcodes.V1_6,
            Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
            className.replaceAll("\\.", "/"),
            null,
            Type.getInternalName(SpreadsheetRangeObject.class),
            null);
        // <init>
        {
            mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                "<init>",
                "(L" + Type.getInternalName(SpreadsheetResultCalculator.class) + ";L" + Type.getInternalName(SpreadsheetCellField.class) + ";L" + Type.getInternalName(SpreadsheetCellField.class) + ";[[L" + Type.getInternalName(IOpenCast.class) + ";)V",
                null,
                null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(1, l0);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitVarInsn(Opcodes.ALOAD, 4);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                Type.getInternalName(SpreadsheetRangeObject.class),
                "<init>",
                "(L" + Type.getInternalName(SpreadsheetResultCalculator.class) + ";L" + Type.getInternalName(SpreadsheetCellField.class) + ";L" + Type.getInternalName(SpreadsheetCellField.class) + ";[[L" + Type.getInternalName(IOpenCast.class) + ";)V");
            mv.visitInsn(Opcodes.RETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", "L" + className.replaceAll("\\.", "/") + ";", null, l0, l1, 0);
            mv.visitMaxs(5, 5);
            mv.visitEnd();
        }
        // autocast
        {
            mv = cw.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                "autocast",
                "(L" + className.replaceAll("\\.", "/") + ";[L" + Type.getInternalName(toClass) + ";)[L" + Type.getInternalName(toClass) + ";",
                null,
                null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitLdcInsn(toClass.getName());
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD,
                className.replaceAll("\\.", "/"),
                "casts",
                "[[L" + Type.getInternalName(IOpenCast.class) + ";");
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                Type.getInternalName(SpreadsheetRangeObject.class),
                "cast",
                "(L" + Type.getInternalName(SpreadsheetRangeObject.class) + ";L" + Type.getInternalName(String.class) + ";[[L" + Type.getInternalName(IOpenCast.class) + ";)Ljava/lang/Object;");
            mv.visitTypeInsn(Opcodes.CHECKCAST, "[L" + Type.getInternalName(toClass) + ";");
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(3, 3);
            mv.visitEnd();

        }
        cw.visitEnd();
        // Create class object.
        //
        ReflectUtils.defineClass(className, cw.toByteArray(), classLoader);
        // Return loaded to classpath class object.
        //
        return Class.forName(className, true, classLoader);
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        try {
            Constructor<?> constructor = this.typeClass.getConstructor(SpreadsheetResultCalculator.class,
                SpreadsheetCellField.class,
                SpreadsheetCellField.class,
                IOpenCast[][].class);
            return constructor.newInstance((SpreadsheetResultCalculator) target, fstart, fend, casts);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        }
    }
}
