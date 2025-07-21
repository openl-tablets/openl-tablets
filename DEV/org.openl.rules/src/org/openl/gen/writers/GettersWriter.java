package org.openl.gen.writers;

import java.util.Map;
import java.util.function.Consumer;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.openl.gen.FieldDescription;
import org.openl.util.ClassUtils;

/**
 * Writes getters to the generated bean class.
 *
 * @author DLiauchuk
 */
public class GettersWriter extends DefaultBeanByteCodeWriter {

    /**
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br>
     *                            (e.g. <code>my/test/TestClass</code>)
     * @param beanFields          fields of generating class.
     */
    public GettersWriter(String beanNameWithPackage, Map<String, FieldDescription> beanFields) {
        super(beanNameWithPackage, null, beanFields);
    }

    @Override
    public void write(ClassWriter classWriter) {
        /*
         * ignore those fields that are of void type. In java it is impossible but possible in Openl, e.g. spreadsheet
         * cell with void type.
         */
        for (Map.Entry<String, FieldDescription> field : getBeanFields().entrySet()) {
            generateGetter(classWriter, field.getKey(), field.getValue());
        }
    }

    /**
     * Generates getter for the fieldEntry.
     *
     * @param classWriter
     * @param fieldName
     * @param fieldDescription
     */
    protected void generateGetter(ClassWriter classWriter, String fieldName, FieldDescription fieldDescription) {
        MethodVisitor methodVisitor;
        String getterName = ClassUtils.getter(fieldName);

        final String javaType = fieldDescription.getTypeDescriptor();
        final String format = "()" + javaType;
        methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, getterName, format, null, null);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, getBeanNameWithPackage(), fieldName, javaType);
        String retClass = fieldDescription.getTypeDescriptor();
        Type type = Type.getType(retClass);
        methodVisitor.visitInsn(type.getOpcode(Opcodes.IRETURN));
        methodVisitor.visitMaxs(0, 0);

        if (fieldDescription.isTransient()) {
            methodVisitor.visitAnnotation("Ljakarta/xml/bind/annotation/XmlTransient;", true).visitEnd();
        }

        if (fieldDescription.getGetterVisitorWriters() != null) {
            for (Consumer<MethodVisitor> methodVisitorConsumer : fieldDescription.getGetterVisitorWriters()) {
                methodVisitorConsumer.accept(methodVisitor);
            }
        }

    }

}
