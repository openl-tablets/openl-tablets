package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.xml.bind.annotation.XmlElement;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import org.openl.gen.ByteCodeUtils;
import org.openl.util.ClassUtils;
import org.openl.util.JavaKeywordUtils;
import org.openl.util.StringUtils;

/**
 * Generates Java Beans byte code for the SpreadsheetResult
 *
 * @author Yury Molchan
 */
final class SpreadsheetResultBeanByteCodeGenerator {
    private static final Method DEFAULT_CONSTRUCTOR = Method.getMethod("void <init> ()");
    private static final String SR_BEAN_CLASS = Type.getDescriptor(SpreadsheetResultBeanClass.class);
    private static final String SPREADSHEET_CELL = Type.getDescriptor(SpreadsheetCell.class);
    private static final String XML_ELEMENT = Type.getDescriptor(XmlElement.class);

    private final String beanNameWithPackage;
    private final List<FieldDescription> fields;


    /**
     * Generates byte code of the Java Bean class for the given set of the fields.
     *
     * @param beanName   name of the generated class, with namespace (e.g. <code>org.opel.generated.spreadsheetresults.MySpr</code>)
     * @param beanFields map of fields, field name as a key, and type as value.
     */
    public static byte[] byteCode(String beanName, List<FieldDescription> beanFields) {
        var jvmClassName = beanName.replace('.', '/');
        return new SpreadsheetResultBeanByteCodeGenerator(jvmClassName, beanFields).getBytes();
    }

    private SpreadsheetResultBeanByteCodeGenerator(String beanNameWithPackage, List<FieldDescription> beanFields) {
        fixDuplicates(beanFields, (field, name) -> field.fieldName = name, field -> field.fieldName);
        fixDuplicates(beanFields, (field, name) -> field.xmlName = name, field -> field.xmlName);
        this.fields = beanFields;
        this.beanNameWithPackage = beanNameWithPackage;
    }

    private byte[] getBytes() {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        visitClassDescription(classWriter);
        visitClassAnnotations(classWriter);
        visitConstructor(classWriter);
        visitFields(classWriter);
        return classWriter.toByteArray();
    }

    private static void fixDuplicates(List<FieldDescription> fields, BiConsumer<FieldDescription, String> set, Function<FieldDescription, String> get) {
        var names = new HashSet<String>(fields.size());
        var duplicates = new ArrayList<FieldDescription>();
        for (var field : fields) {
            if (!names.add(get.apply(field))) {
                duplicates.add(field);
            }
        }
        for (var field : duplicates) {
            var name = get.apply(field);
            int i = 1;
            while (names.contains(name + i)) {
                i++;
            }
            String newName = name + i;
            set.accept(field, newName);
            names.add(newName);
        }
    }

    private void visitClassDescription(ClassWriter classWriter) {
        classWriter.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                beanNameWithPackage,
                null,
                "java/lang/Object",
                new String[]{"java/io/Serializable"});
    }

    private void visitClassAnnotations(ClassWriter classWriter) {
        String namespace = ByteCodeUtils.getNamespace(beanNameWithPackage);
        String name = beanNameWithPackage.substring(beanNameWithPackage.lastIndexOf('/') + 1);
        var av = classWriter.visitAnnotation("Ljavax/xml/bind/annotation/XmlRootElement;", true);
        av.visit("namespace", namespace);
        av.visit("name", name);
        av.visitEnd();

        av = classWriter.visitAnnotation("Ljavax/xml/bind/annotation/XmlAccessorType;", true);
        av.visitEnum("value", "Ljavax/xml/bind/annotation/XmlAccessType;", "PROPERTY");
        av.visitEnd();

        av = classWriter.visitAnnotation(SR_BEAN_CLASS, true);
        av.visitEnd();

        av = classWriter.visitAnnotation("Ljavax/xml/bind/annotation/XmlType;", true);
        av.visit("namespace", namespace);
        av.visit("name", name);
        AnnotationVisitor av1 = av.visitArray("propOrder");
        for (var e : fields) {
            av1.visit(null, e.fieldName);
        }
        av1.visitEnd();
        av.visitEnd();
    }

    private void visitConstructor(ClassWriter classWriter) {
        var mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC, DEFAULT_CONSTRUCTOR, null, null, classWriter);

        // invokes the super class constructor
        mg.loadThis();
        mg.invokeConstructor(Type.getType(Object.class), DEFAULT_CONSTRUCTOR);
        mg.returnValue();
        mg.endMethod();
    }

    private void visitFields(ClassWriter classWriter) {
        for (var field : fields) {
            classWriter.visitField((Opcodes.ACC_PRIVATE), field.fieldName, field.fieldType, null, null).visitEnd();
            generateGetter(classWriter, field.fieldName, field);
            generateSetter(classWriter, field.fieldName, field.fieldType);
        }
    }

    private void generateGetter(ClassWriter classWriter, String fieldName, FieldDescription fieldDescription) {
        String getterName = ClassUtils.getter(fieldName);
        final String methodDescriptor = "()" + fieldDescription.fieldType;
        var methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, getterName, methodDescriptor, null, null);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, beanNameWithPackage, fieldName, fieldDescription.fieldType);
        methodVisitor.visitInsn(Type.getType(fieldDescription.fieldType).getOpcode(Opcodes.IRETURN));
        methodVisitor.visitMaxs(0, 0);

        var av = methodVisitor.visitAnnotation(SPREADSHEET_CELL, true);
        if (fieldDescription.column != null) {
            av.visit("column", fieldDescription.column);

        }
        if (fieldDescription.row != null) {
            av.visit("row", fieldDescription.row);
        }

        if (fieldDescription.FIX_ME) {
            // Strange behavior. EPBDS-9437 OpenAPI schema test is failing without it.
            // Driver_Forms property is disappeared from the AnySpreadsheetResult component.
            av.visit("FIX_ME", true); // FIXME: AnySpreadsheetResult in OpenAPI
        }
        av.visitEnd();

        av = methodVisitor.visitAnnotation(XML_ELEMENT, true);
        av.visit("name", fieldDescription.xmlName);
        av.visitEnd();

        methodVisitor.visitEnd();
    }

    private void generateSetter(ClassWriter classWriter, String fieldName, String fieldType) {
        String setterName = ClassUtils.setter(fieldName);
        String methodDescriptor = "(" + fieldType + ")V";
        var methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, setterName, methodDescriptor, null, null);

        Label l0 = new Label();
        methodVisitor.visitLabel(l0);

        // this.fieldName = arg0
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        int constantForVarInsn = Type.getType(fieldType).getOpcode(Opcodes.ILOAD);
        methodVisitor.visitVarInsn(constantForVarInsn, 1);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, beanNameWithPackage, fieldName, fieldType);
        methodVisitor.visitInsn(Opcodes.RETURN);

        // Add variable name to DEBUG
        Label l2 = new Label();
        methodVisitor.visitLabel(l2);
        methodVisitor.visitLocalVariable(fieldName, fieldType, null, l0, l2, 1);

        methodVisitor.visitMaxs(0, 0);
    }

    static final class FieldDescription {
        final String className;
        final String fieldType;
        final String row;
        final String column;
        final boolean FIX_ME;
        String fieldName;
        String xmlName;

        FieldDescription(String canonicalClassName, String row, String column, boolean FIX_ME) {
            this.className = canonicalClassName;
            this.fieldType = ByteCodeUtils.toTypeDescriptor(canonicalClassName);
            this.row = row;
            this.column = column;
            this.FIX_ME = FIX_ME;
            String fieldName;
            if (column == null) {
                fieldName = JavaKeywordUtils.toJavaIdentifier(row);
                xmlName = fieldName;
            } else if (row == null) {
                fieldName = JavaKeywordUtils.toJavaIdentifier(column);
                xmlName = fieldName;
            } else {
                var c = JavaKeywordUtils.toJavaIdentifier(column);
                var r = JavaKeywordUtils.toJavaIdentifier(row);
                fieldName = c + StringUtils.capitalize(r);
                xmlName = c + "_" + r;
            }
            if (fieldName.isEmpty()) {
                fieldName = "_";
                xmlName = fieldName;
            }
            this.fieldName = ClassUtils.decapitalize(fieldName);
        }
    }
}