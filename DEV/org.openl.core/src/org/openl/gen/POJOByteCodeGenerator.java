package org.openl.gen;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.gen.writers.BeanByteCodeWriter;
import org.openl.gen.writers.ConstructorWithParametersWriter;
import org.openl.gen.writers.DefaultConstructorWriter;
import org.openl.gen.writers.EqualsWriter;
import org.openl.gen.writers.GettersWriter;
import org.openl.gen.writers.HashCodeWriter;
import org.openl.gen.writers.ISO8601DateFormater;
import org.openl.gen.writers.SettersWriter;
import org.openl.gen.writers.ToStringWriter;
import org.openl.runtime.ContextProperty;
import org.openl.util.JAXBUtils;

/**
 * Generates byte code for simple java bean.
 *
 * @author Yury Molchan, Marat Kamalov
 */
public class POJOByteCodeGenerator {

    private final String beanNameWithPackage;
    private final Class<?> parentClass;
    private Map<String, FieldDescription> beanFields;
    private Map<String, FieldDescription> parentFields;
    private List<BeanByteCodeWriter> writers;
    private boolean publicFields;

    /**
     *
     * @param beanName name of the generated class, with namespace (e.g. <code>my.test.TestClass</code>)
     * @param beanFields map of fields, field name as a key, and type as value.
     * @param parentClass parent class
     * @param additionalConstructor true if required to generate constructor with parameter
     */
    public POJOByteCodeGenerator(String beanName,
            Map<String, FieldDescription> beanFields,
            Class<?> parentClass,
            Map<String, FieldDescription> parentFields,
            boolean additionalConstructor,
            boolean publicFields) {

        this.beanFields = new LinkedHashMap<>(beanFields);
        this.parentClass = parentClass;
        this.parentFields = new LinkedHashMap<>(parentFields);
        this.beanNameWithPackage = beanName.replace('.', '/');
        this.publicFields = publicFields;

        Map<String, FieldDescription> allFields = new LinkedHashMap<>();
        allFields.putAll(parentFields);
        allFields.putAll(beanFields);

        this.writers = new ArrayList<>();
        writers.add(new DefaultConstructorWriter(beanNameWithPackage, parentClass, this.beanFields));
        if (additionalConstructor && allFields.size() < 256 && allFields.size() > 0) {
            // Generate constructor with parameters only in case where there are
            // less than 256 arguments.
            // 255 arguments to the method is a Java limitation
            //
            writers.add(new ConstructorWithParametersWriter(beanNameWithPackage,
                parentClass,
                this.beanFields,
                this.parentFields,
                allFields));
        }

        writers.add(new GettersWriter(beanNameWithPackage, this.beanFields));
        writers.add(new SettersWriter(beanNameWithPackage, this.beanFields));
        writers.add(new ToStringWriter(beanNameWithPackage, allFields));
        writers.add(new EqualsWriter(beanNameWithPackage, allFields));
        writers.add(new HashCodeWriter(beanNameWithPackage, allFields));
    }

    private void visitClassDescription(ClassWriter classWriter) {
        String parent = Type.getInternalName(parentClass);
        classWriter.visit(Opcodes.V1_8,
            Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
            beanNameWithPackage,
            null,
            parent,
            getDefaultInterfaces());
    }

    protected String[] getDefaultInterfaces() {
        return new String[] { "java/io/Serializable" };
    }

    private void visitJAXBAnnotations(ClassWriter classWriter) {
        String namespace = getNamespace(beanNameWithPackage);
        String name = beanNameWithPackage.substring(beanNameWithPackage.lastIndexOf('/') + 1);

        AnnotationVisitor av = classWriter.visitAnnotation("Ljavax/xml/bind/annotation/XmlRootElement;", true);
        av.visit("namespace", namespace);
        av.visit("name", name);
        av.visitEnd();

        av = classWriter.visitAnnotation("Ljavax/xml/bind/annotation/XmlAccessorType;", true);
        av.visitEnum("value", "Ljavax/xml/bind/annotation/XmlAccessType;", "FIELD");
        av.visitEnd();

        av = classWriter.visitAnnotation("Ljavax/xml/bind/annotation/XmlType;", true);
        av.visit("namespace", namespace);
        av.visit("name", name);
        AnnotationVisitor av1 = av.visitArray("propOrder");
        for (Entry<String, FieldDescription> e : parentFields.entrySet()) {
            av1.visit(null, e.getKey());
        }
        for (Entry<String, FieldDescription> e : beanFields.entrySet()) {
            av1.visit(null, e.getKey());
        }
        av1.visitEnd();
        av.visitEnd();
    }

    private void visitJAXBAnnotationsOnField(FieldVisitor fieldVisitor,
            String fieldName,
            FieldDescription field,
            String javaType) {
        AnnotationVisitor av = fieldVisitor.visitAnnotation("Ljavax/xml/bind/annotation/XmlElement;", true);
        av.visit("name", fieldName);

        if (field.hasDefaultValue() && field.getTypeDescriptor().length() != 1) {
            av.visit("nillable", true);
        }
        if (field.hasDefaultValue() && !field.hasDefaultKeyWord()) {
            String defaultFieldValue = field.getDefaultValueAsString();
            if (Boolean.class.getName().equals(field.getTypeName()) || boolean.class.getName()
                .equals(field.getTypeName())) {
                defaultFieldValue = String.valueOf(field.getDefaultValue());
            } else if (field.getTypeName().equals(Date.class.getName())) {
                Date date = (Date) field.getDefaultValue();
                defaultFieldValue = ISO8601DateFormater.format(date);
            }
            av.visit("defaultValue", defaultFieldValue);
        }
        try {
            String componentJavaType = javaType.replaceAll("\\[", "");
            String clsName = Type.getType(componentJavaType).getClassName();
            Class<?> type = Thread.currentThread().getContextClassLoader().loadClass(clsName);
            if (type.isInterface() && !Map.class.isAssignableFrom(type) && !Collection.class.isAssignableFrom(type)) {
                int d = javaType.length() - componentJavaType.length();
                Class<?> g = JAXBUtils.extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(type);
                Class<?> useType = g != null ? g : Object.class;
                if (d > 0) {
                    useType = Array.newInstance(Object.class, new int[d]).getClass();
                }
                av.visit("type", Type.getType(useType));
            }
        } catch (Exception ignored) {
        }
        av.visitEnd();
    }

    private void visitFields(ClassWriter classWriter) {
        for (Map.Entry<String, FieldDescription> field : beanFields.entrySet()) {
            String fieldTypeName = field.getValue().getTypeDescriptor();
            FieldVisitor fieldVisitor = classWriter
                .visitField(publicFields ? Opcodes.ACC_PUBLIC
                                         : Opcodes.ACC_PROTECTED,
                    field.getKey(),
                    fieldTypeName,
                    null,
                    null);
            if (field.getValue().isContextProperty()) {
                visitOpenLContextAnnotation(field.getValue().getContextPropertyName(), fieldVisitor);
            }
            visitJAXBAnnotationsOnField(fieldVisitor, field.getKey(), field.getValue(), fieldTypeName);
        }
    }

    private void visitOpenLContextAnnotation(String fieldName, FieldVisitor fieldVisitor) {
        AnnotationVisitor annotationVisitor = fieldVisitor.visitAnnotation(Type.getDescriptor(ContextProperty.class),
            true);
        annotationVisitor.visit("value", fieldName);
        annotationVisitor.visitEnd();
    }

    private static String getNamespace(String beanNameWithPackage) {
        String[] parts = beanNameWithPackage.split("/");
        StringBuilder builder = new StringBuilder("http://");
        for (int i = parts.length - 2; i >= 0; i--) {
            builder.append(parts[i]);
            if (i != 0) {
                builder.append(".");
            }
        }
        return builder.toString();
    }

    protected void visitExtraByteCodeGeneration(ClassWriter classWriter) {

    }

    /**
     * Return loaded to classpath class object
     *
     * @return <code>Class<?></code> descriptor for given byteCode
     */
    public byte[] byteCode() {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        visitClassDescription(classWriter);

        visitJAXBAnnotations(classWriter);

        visitFields(classWriter);

        for (BeanByteCodeWriter writer : writers) {
            writer.write(classWriter);
        }

        visitExtraByteCodeGeneration(classWriter);

        return classWriter.toByteArray();
    }

    protected Map<String, FieldDescription> getBeanFields() {
        return beanFields;
    }

    protected String getBeanNameDescriptor() {
        return 'L' + beanNameWithPackage + ';';
    }
}
