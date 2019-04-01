package org.openl.rules.datatype.gen;

import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.openl.gen.FieldDescription;
import org.openl.gen.POJOByteCodeGenerator;

class SimpleBeanByteCodeGenerator extends POJOByteCodeGenerator {

    private final String methodName;

    SimpleBeanByteCodeGenerator(String beanName,
            LinkedHashMap<String, FieldDescription> beanFields,
            Class<?> parentClass,
            Map<String, FieldDescription> parentFields,
            String methodName) {
        super(beanName, beanFields, parentClass, parentFields, true);
        this.methodName = methodName;
    }

    private void addArgs(ClassWriter classWriter, Map<String, FieldDescription> beanFields) {
        Type objectType = Type.getType(Object.class);
        Type beanType = Type.getType(getBeanNameDescriptor());

        Method args = Method.getMethod("java.lang.Object[] _args()");
        GeneratorAdapter ag = new GeneratorAdapter(Opcodes.ACC_PUBLIC, args, null, null, classWriter);
        ag.push(beanFields.size()); // array length
        ag.newArray(objectType); // ar = new Object[size]

        int i = 0;
        for (Map.Entry<String, FieldDescription> field : beanFields.entrySet()) {
            Type fieldType = Type.getType(field.getValue().getTypeDescriptor());

            ag.dup();// ar
            ag.push(i); // index
            ag.loadThis(); // this.
            ag.getField(beanType, field.getKey(), fieldType); // field
            ag.valueOf(fieldType); // value = Type.valueOf(this.field)
            ag.arrayStore(objectType); // ar[i]=value;

            i++;
        }
        ag.returnValue();
        ag.endMethod();
    }

    private void addTypes(ClassWriter classWriter, Map<String, FieldDescription> beanFields) {
        Type classType = Type.getType(Class.class);

        Method types = Method.getMethod("java.lang.Class[] _types()");
        GeneratorAdapter tg = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            types,
            null,
            null,
            classWriter);
        tg.push(beanFields.size()); // array length
        tg.newArray(classType); // ar = new Object[size]

        int i = 0;
        for (Map.Entry<String, FieldDescription> field : beanFields.entrySet()) {
            Type fieldType = Type.getType(field.getValue().getTypeDescriptor());

            tg.dup();// ar
            tg.push(i); // index
            tg.push(fieldType); // value = Type.class
            tg.arrayStore(classType); // ar[i]=value;

            i++;
        }

        tg.returnValue();
        tg.endMethod();
    }

    private void addMethod(ClassWriter classWriter, String methodName) {
        Method method = Method.getMethod("java.lang.String _method()");
        GeneratorAdapter mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            method,
            null,
            null,
            classWriter);
        mg.push(methodName);
        mg.returnValue();
        mg.endMethod();
    }

    @Override
    protected void visitExtraByteCodeGeneration(ClassWriter classWriter) {
        if (methodName != null) {
            addArgs(classWriter, getBeanFields());
            addTypes(classWriter, getBeanFields());
            addMethod(classWriter, methodName);
        }
    }
}
