package org.openl.rules.datatype.gen;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.bean.writers.BeanByteCodeWriter;
import org.openl.rules.datatype.gen.bean.writers.ConstructorWithParametersWriter;
import org.openl.rules.datatype.gen.bean.writers.DefaultConstructorWriter;
import org.openl.rules.datatype.gen.bean.writers.DefaultValue;
import org.openl.rules.datatype.gen.bean.writers.EqualsWriter;
import org.openl.rules.datatype.gen.bean.writers.GettersWriter;
import org.openl.rules.datatype.gen.bean.writers.HashCodeWriter;
import org.openl.rules.datatype.gen.bean.writers.ISO8601DateFormater;
import org.openl.rules.datatype.gen.bean.writers.SettersWriter;
import org.openl.rules.datatype.gen.bean.writers.ToStringWriter;

/**
 * Generates byte code for simple java bean.
 * 
 * @author DLiauchuk
 *
 */
public class SimpleBeanByteCodeGenerator {

    private byte[] bytes;

    /**
     * 
     * @param beanName name of the generated class, with namespace (e.g. <code>my.test.TestClass</code>)
     * @param beanFields map of fields, field name as a key, and type as value.
     * @param parentClass parent class
     */
    public SimpleBeanByteCodeGenerator(String beanName,
            Map<String, FieldDescription> beanFields,
            Class<?> parentClass,
            Map<String, FieldDescription> parentFields) {

        String beanNameWithPackage = beanName.replace('.', '/');
        LinkedHashMap<String, FieldDescription> allFields = new LinkedHashMap<String, FieldDescription>();
        allFields.putAll(parentFields);
        allFields.putAll(beanFields);

        List<BeanByteCodeWriter> writers = new ArrayList<BeanByteCodeWriter>();
        writers.add(new DefaultConstructorWriter(beanNameWithPackage, parentClass, beanFields));
        if (allFields.size() < 256) {
            // Generate constructor with parameters only in case where there are
            // less than 256 arguments.
            // 255 arguments to the method is a Java limitation
            //
            writers.add(new ConstructorWithParametersWriter(beanNameWithPackage,
                parentClass,
                beanFields,
                parentFields,
                allFields));
        }
        writers.add(new GettersWriter(beanNameWithPackage, beanFields));
        writers.add(new SettersWriter(beanNameWithPackage, beanFields));
        writers.add(new ToStringWriter(beanNameWithPackage, allFields));
        writers.add(new EqualsWriter(beanNameWithPackage, allFields));
        writers.add(new HashCodeWriter(beanNameWithPackage, allFields));
        /** generate byte code */
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        visitClassDescription(classWriter, beanNameWithPackage, parentClass);
        visitJAXBAnnotation(classWriter, beanNameWithPackage);
        visitFields(classWriter, beanFields);

        for (BeanByteCodeWriter writer : writers) {
            writer.write(classWriter);
        }

        bytes = classWriter.toByteArray();
    }

    private static void visitClassDescription(ClassWriter classWriter, String className, Class<?> parentClass) {
        String[] interfaces = { "java/io/Serializable" };
        String parent = Type.getInternalName(parentClass);
        classWriter.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, className, null, parent, interfaces);
    }

    private static void visitJAXBAnnotation(ClassWriter classWriter, String beannameWithPackage) {
        String namespace = getNamespace(beannameWithPackage);

        AnnotationVisitor av = classWriter.visitAnnotation(Type.getDescriptor(XmlRootElement.class), true);
        av.visit("namespace", namespace);
        av.visitEnd();

        av = classWriter.visitAnnotation(Type.getDescriptor(XmlType.class), true);
        av.visit("namespace", namespace);
        av.visitEnd();

    }

    private static void visitFields(ClassWriter classWriter, Map<String, FieldDescription> beanFields) {
        for (Map.Entry<String, FieldDescription> field : beanFields.entrySet()) {
            String fieldTypeName = field.getValue().getTypeDescriptor();
            FieldVisitor fieldVisitor = classWriter
                    .visitField(Opcodes.ACC_PROTECTED, field.getKey(), fieldTypeName, null, null);
            if (field.getValue().hasDefaultValue()) {
                // Requred for java class generation
                AnnotationVisitor annotationVisitor = fieldVisitor
                        .visitAnnotation(Type.getDescriptor(DefaultValue.class), true);
                if (field.getValue().getTypeName().equals(Date.class.getName())) {
                    Object value = field.getValue().getDefaultValue();
                    if (value instanceof Date) {
                        Date date = (Date) value;
                        String formatedDate = ISO8601DateFormater.format(date);
                        annotationVisitor.visit("value", formatedDate);
                    }
                } else {
                    annotationVisitor.visit("value", field.getValue().getDefaultValueAsString());
                }
                annotationVisitor.visitEnd();
            }
        }
    }

    private static String getNamespace(String beannameWithPackage) {
        String[] parts = beannameWithPackage.split("/");
        StringBuilder builder = new StringBuilder("http://");
        for (int i = parts.length - 2; i >= 0; i--) {
            builder.append(parts[i]);
            if (i != 0) {
                builder.append(".");
            }
        }
        return builder.toString();
    }

    /**
     * Return loaded to classpath class object
     *
     * @return <code>Class<?></code> descriptor for given byteCode
     */
    public byte[] byteCode() {
        return bytes;
    }
}
