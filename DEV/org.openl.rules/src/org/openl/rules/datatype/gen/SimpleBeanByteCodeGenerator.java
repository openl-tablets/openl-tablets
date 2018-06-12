package org.openl.rules.datatype.gen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import org.openl.rules.datatype.gen.bean.writers.BeanByteCodeWriter;
import org.openl.rules.datatype.gen.bean.writers.ConstructorWithParametersWriter;
import org.openl.rules.datatype.gen.bean.writers.DefaultConstructorWriter;
import org.openl.rules.datatype.gen.bean.writers.EqualsWriter;
import org.openl.rules.datatype.gen.bean.writers.GettersWriter;
import org.openl.rules.datatype.gen.bean.writers.HashCodeWriter;
import org.openl.rules.datatype.gen.bean.writers.SettersWriter;
import org.openl.rules.datatype.gen.bean.writers.ToStringWriter;

/**
 * Generates byte code for simple java bean.
 * 
 * @author DLiauchuk
 *
 */
class SimpleBeanByteCodeGenerator {

    private byte[] bytes;

    /**
     * 
     * @param beanName name of the generated class, with namespace (e.g. <code>my.test.TestClass</code>)
     * @param beanFields map of fields, field name as a key, and type as value.
     * @param parentClass parent class
     */
    SimpleBeanByteCodeGenerator(String beanName,
            LinkedHashMap<String, FieldDescription> beanFields,
            Class<?> parentClass,
            Map<String, FieldDescription> parentFields,
            String methodName) {

        String beanNameWithPackage = beanName.replace('.', '/');
        LinkedHashMap<String, FieldDescription> allFields = new LinkedHashMap<>();
        allFields.putAll(parentFields);
        allFields.putAll(beanFields);

        List<BeanByteCodeWriter> writers = new ArrayList<>();
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
        /* generate byte code */
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        visitClassDescription(classWriter, beanNameWithPackage, parentClass);
        visitJAXBAnnotation(classWriter, beanNameWithPackage);
        visitFields(classWriter, beanFields);

        for (BeanByteCodeWriter writer : writers) {
            writer.write(classWriter);
        }

        if (methodName != null) {
            add_args(classWriter, beanFields, beanNameWithPackage);
            add_types(classWriter, beanFields, beanNameWithPackage);
            add_method(classWriter, methodName);
        }

        bytes = classWriter.toByteArray();
    }

    private void add_args(ClassWriter classWriter, LinkedHashMap<String, FieldDescription> beanFields, String beanNameWithPackage) {
        Type OBJECT_TYPE = Type.getType(Object.class);
        Type beanType = Type.getType(beanNameWithPackage);

        Method _args = Method.getMethod("java.lang.Object[] _args()");
        GeneratorAdapter ag = new GeneratorAdapter(Opcodes.ACC_PUBLIC, _args, null, null, classWriter);
        ag.push(beanFields.size()); // array length
        ag.newArray(OBJECT_TYPE); // ar = new Object[size]

        int i = 0;
        for (Map.Entry<String, FieldDescription> field : beanFields.entrySet()) {
            Type fieldType = Type.getType(field.getValue().getTypeDescriptor());

            ag.dup();// ar
            ag.push(i); // index
            ag.loadThis(); // this.
            ag.getField(beanType, field.getKey(), fieldType); // field
            ag.valueOf(fieldType); // value = Type.valueOf(this.field)
            ag.arrayStore(OBJECT_TYPE); // ar[i]=value;

            i++;
        }
        ag.returnValue();
        ag.endMethod();
    }

    private void add_types(ClassWriter classWriter, LinkedHashMap<String, FieldDescription> beanFields, String beanNameWithPackage) {
        Type CLASS_TYPE = Type.getType(Class.class);

        Method _types = Method.getMethod("java.lang.Class[] _types()");
        GeneratorAdapter tg = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, _types, null, null, classWriter);
        tg.push(beanFields.size()); // array length
        tg.newArray(CLASS_TYPE); // ar = new Object[size]

        int i = 0;
        for (Map.Entry<String, FieldDescription> field : beanFields.entrySet()) {
            Type fieldType = Type.getType(field.getValue().getTypeDescriptor());

            tg.dup();// ar
            tg.push(i); // index
            tg.push(fieldType); // value = Type.class
            tg.arrayStore(CLASS_TYPE); // ar[i]=value;

            i++;
        }

        tg.returnValue();
        tg.endMethod();
    }

    private void add_method(ClassWriter classWriter, String methodName) {
        Method _method = Method.getMethod("java.lang.String _method()");
        GeneratorAdapter mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, _method, null, null, classWriter);
        mg.push(methodName);
        mg.returnValue();
        mg.endMethod();
    }

    private static void visitClassDescription(ClassWriter classWriter, String className, Class<?> parentClass) {
        String[] interfaces = { "java/io/Serializable" };
        String parent = Type.getInternalName(parentClass);
        classWriter.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, className, null, parent, interfaces);
    }

    private static void visitJAXBAnnotation(ClassWriter classWriter, String beannameWithPackage) {
        String namespace = getNamespace(beannameWithPackage);
        String name = beannameWithPackage.substring(beannameWithPackage.lastIndexOf('/') + 1);

        AnnotationVisitor av = classWriter.visitAnnotation("Ljavax/xml/bind/annotation/XmlRootElement;", true);
        av.visit("namespace", namespace);
        av.visit("name", name);
        av.visitEnd();

        av = classWriter.visitAnnotation("Ljavax/xml/bind/annotation/XmlType;", true);
        av.visit("namespace", namespace);
        av.visit("name", name);
        av.visitEnd();
    }

    private static void visitFields(ClassWriter classWriter, Map<String, FieldDescription> beanFields) {
        for (Map.Entry<String, FieldDescription> field : beanFields.entrySet()) {
            String fieldTypeName = field.getValue().getTypeDescriptor();
            classWriter.visitField(Opcodes.ACC_PROTECTED, field.getKey(), fieldTypeName, null, null);
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
    byte[] byteCode() {
        return bytes;
    }
}
