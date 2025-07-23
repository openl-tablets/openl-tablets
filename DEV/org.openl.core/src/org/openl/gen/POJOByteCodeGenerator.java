package org.openl.gen;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

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
    public final static TypeDescription OBJECT_TYPE_DESCRIPTION = new TypeDescription(Object.class.getName());

    private final String beanNameWithPackage;
    private final TypeDescription parentType;
    private final Map<String, FieldDescription> fields;
    private final Map<String, FieldDescription> parentFields;
    private final List<BeanByteCodeWriter> writers;
    private final boolean publicFields;
    private final Set<Consumer<ClassWriter>> typeWriters;

    /**
     * @param beanName              name of the generated class, with namespace (e.g. <code>my.test.TestClass</code>)
     * @param beanFields            map of fields, field name as a key, and type as value.
     * @param parentType            parent type
     * @param additionalConstructor true if required to generate constructor with parameter
     */
    public POJOByteCodeGenerator(String beanName,
                                 Map<String, FieldDescription> beanFields,
                                 TypeDescription parentType,
                                 Map<String, FieldDescription> parentFields,
                                 Set<Consumer<ClassWriter>> typeWriters,
                                 boolean additionalConstructor,
                                 boolean equalsHashCodeToStringMethods,
                                 boolean publicFields) {

        this.fields = beanFields != null ? beanFields : Collections.emptyMap();
        this.parentType = parentType;
        this.parentFields = parentFields != null ? parentFields : Collections.emptyMap();
        this.beanNameWithPackage = beanName.replace('.', '/');
        this.publicFields = publicFields;
        this.typeWriters = typeWriters != null ? new LinkedHashSet<>(typeWriters) : Collections.emptySet();

        Map<String, FieldDescription> allFields = new LinkedHashMap<>();
        allFields.putAll(this.parentFields);
        allFields.putAll(this.fields);

        this.writers = new ArrayList<>();
        writers.add(new DefaultConstructorWriter(beanNameWithPackage, parentType, this.fields));
        if (additionalConstructor && allFields.size() > 0 && isFollowJavaSpecification(
                allFields) && !OBJECT_TYPE_DESCRIPTION.getTypeName().equals(parentType.getTypeDescriptor())) {
            // Generate constructor with parameters only in case where there are
            // less than 256 arguments.
            // 255 arguments to the method is a Java limitation
            //
            writers.add(
                    new ConstructorWithParametersWriter(beanNameWithPackage, parentType, this.parentFields, this.fields));
        }

        if (!publicFields) {
            writers.add(new GettersWriter(beanNameWithPackage, this.fields));
            writers.add(new SettersWriter(beanNameWithPackage, this.fields));
        }
        if (equalsHashCodeToStringMethods) {
            writers.add(new ToStringWriter(beanNameWithPackage, allFields));
            writers.add(new EqualsWriter(beanNameWithPackage, allFields));
            writers.add(new HashCodeWriter(beanNameWithPackage, allFields));
        }
    }

    private boolean isFollowJavaSpecification(Map<String, FieldDescription> allFields) {
        int max = 254;
        for (FieldDescription fieldDescription : allFields.values()) {
            // if type uses more than 4 bytes then reduce a size for one
            if ("long".equals(fieldDescription.getTypeName()) || "double".equals(fieldDescription.getTypeName())) {
                max--;
            }
        }
        return allFields.size() <= max;
    }

    private void visitClassDescription(ClassWriter classWriter) {
        String parent = parentType.getTypeName().replace('.', '/');
        classWriter.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                beanNameWithPackage,
                null,
                parent,
                getDefaultInterfaces());
    }

    protected String[] getDefaultInterfaces() {
        return new String[]{"java/io/Serializable"};
    }

    private void visitTypeWriters(ClassWriter classWriter) {
        for (Consumer<ClassWriter> writer : typeWriters) {
            writer.accept(classWriter);
        }
    }

    private void visitJAXBAnnotations(ClassWriter classWriter) {
        String namespace = ByteCodeUtils.getNamespace(beanNameWithPackage);
        String name = beanNameWithPackage.substring(beanNameWithPackage.lastIndexOf('/') + 1);

        AnnotationVisitor av = classWriter.visitAnnotation("Ljakarta/xml/bind/annotation/XmlRootElement;", true);
        av.visit("namespace", namespace);
        av.visit("name", name);
        av.visitEnd();

        av = classWriter.visitAnnotation("Ljakarta/xml/bind/annotation/XmlAccessorType;", true);
        av.visitEnum("value", "Ljakarta/xml/bind/annotation/XmlAccessType;", "FIELD");
        av.visitEnd();

        av = classWriter.visitAnnotation("Ljakarta/xml/bind/annotation/XmlType;", true);
        av.visit("namespace", namespace);
        av.visit("name", name);
        AnnotationVisitor av1 = av.visitArray("propOrder");
        for (Entry<String, FieldDescription> e : parentFields.entrySet()) {
            if (!e.getValue().isTransient()) {
                av1.visit(null, e.getKey());
            }
        }
        for (Entry<String, FieldDescription> e : fields.entrySet()) {
            if (!e.getValue().isTransient()) {
                av1.visit(null, e.getKey());
            }
        }
        av1.visitEnd();
        av.visitEnd();
    }

    private void visitJAXBAnnotationsOnField(FieldVisitor fieldVisitor,
                                             String fieldName,
                                             FieldDescription field,
                                             String javaType) {
        AnnotationVisitor av = fieldVisitor.visitAnnotation("Ljakarta/xml/bind/annotation/XmlElement;", true);

        av.visit("name", field.getXmlName() != null ? field.getXmlName() : fieldName);

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

        if (field.isTransient()) {
            fieldVisitor.visitAnnotation("Ljakarta/xml/bind/annotation/XmlTransient;", true).visitEnd();
        }
    }

    private void visitFields(ClassWriter classWriter) {
        int i = 0;
        for (Map.Entry<String, FieldDescription> field : fields.entrySet()) {
            String fieldTypeName = field.getValue().getTypeDescriptor();
            int acc = (publicFields ? Opcodes.ACC_PUBLIC
                    : Opcodes.ACC_PROTECTED) | (field.getValue().isTransient() ? Opcodes.ACC_TRANSIENT
                    : 0);
            FieldVisitor fieldVisitor = classWriter.visitField(acc, field.getKey(), fieldTypeName, null, null);

            if (field.getValue().isContextProperty()) {
                visitOpenLContextAnnotation(field.getValue().getContextPropertyName(), fieldVisitor);
            }

            if (field.getValue().getFieldVisitorWriters() != null) {
                for (Consumer<FieldVisitor> fieldVisitorConsumer : field.getValue().getFieldVisitorWriters()) {
                    fieldVisitorConsumer.accept(fieldVisitor);
                }
            }

            visitJAXBAnnotationsOnField(fieldVisitor, field.getKey(), field.getValue(), fieldTypeName);
            visitFieldVisitor(fieldVisitor, field.getKey(), field.getValue(), fieldTypeName, i);
            i++;
        }
    }

    protected void visitFieldVisitor(FieldVisitor fieldVisitor,
                                     String fieldName,
                                     FieldDescription field,
                                     String javaType,
                                     int index) {
    }

    private void visitOpenLContextAnnotation(String fieldName, FieldVisitor fieldVisitor) {
        AnnotationVisitor annotationVisitor = fieldVisitor.visitAnnotation(Type.getDescriptor(ContextProperty.class),
                true);
        annotationVisitor.visit("value", fieldName);
        annotationVisitor.visitEnd();
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

        visitTypeWriters(classWriter);

        visitFields(classWriter);

        for (BeanByteCodeWriter writer : writers) {
            writer.write(classWriter);
        }

        visitExtraByteCodeGeneration(classWriter);

        return classWriter.toByteArray();
    }

    protected Map<String, FieldDescription> getFields() {
        return fields;
    }

    protected String getBeanNameDescriptor() {
        return 'L' + beanNameWithPackage + ';';
    }
}
