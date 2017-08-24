package org.openl.rules.datatype.gen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.bean.writers.*;

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
        writers.add(new JAXBAnnotationWriter(beanNameWithPackage));
        writers.add(new ProtectedFieldsWriter(beanFields));
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

    /**
     * Return loaded to classpath class object
     *
     * @return <code>Class<?></code> descriptor for given byteCode
     */
    public byte[] byteCode() {
        return bytes;
    }
}
