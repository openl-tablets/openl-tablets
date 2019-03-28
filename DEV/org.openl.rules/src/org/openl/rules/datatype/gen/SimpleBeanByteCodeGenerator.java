package org.openl.rules.datatype.gen;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.openl.gen.FieldDescription;
import org.openl.gen.POJOByteCodeGenerator;

import java.util.LinkedHashMap;
import java.util.Map;

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

    private void add_args(ClassWriter classWriter, Map<String, FieldDescription> beanFields) {
        Type OBJECT_TYPE = Type.getType(Object.class);
        Type beanType = Type.getType(getBeanNameDescriptor());

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

    private void add_types(ClassWriter classWriter, Map<String, FieldDescription> beanFields) {
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

    @Override
    protected void visitExtraByteCodeGeneration(ClassWriter classWriter) {
        if (methodName != null) {
            add_args(classWriter, getBeanFields());
            add_types(classWriter, getBeanFields());
            add_method(classWriter, methodName);
        }
    }
}
