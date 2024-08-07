package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiFunction;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import org.openl.cache.GenericKey;
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
    public static final Type GENERIC_KEY_TYPE = Type.getType(GenericKey.class);
    public static final Method GENERIC_KEY_CREATOR = Method.getMethod("org.openl.cache.GenericKey getInstance(Object, Object)");
    public static final Type VALUES_TYPE = Type.getType(HashMap.class);
    public static final Method GET_VALUE = Method.getMethod("Object get(Object)");
    public static final Method SET_VALUE = Method.getMethod("Object put(Object, Object)");
    public static final Type SR_TYPE = Type.getType(SpreadsheetResult.class);
    public static final Method SR_GET_VALUE = Method.getMethod("Object getValue(String, String)");

    private final String beanNameWithPackage;
    private final List<FieldDescription> fields;
    private final Type beanType;
    private final Method setMethod;


    /**
     * Generates byte code of the Java Bean class for the given set of the fields.
     *
     * @param beanName   name of the generated class, with namespace (e.g. <code>org.opel.generated.spreadsheetresults.MySpr</code>)
     * @param beanFields map of fields, field name as a key, and type as value.
     */
    public static byte[] byteCode(String beanName, List<FieldDescription> beanFields) {
        return new SpreadsheetResultBeanByteCodeGenerator(beanName, beanFields).getBytes();
    }

    private SpreadsheetResultBeanByteCodeGenerator(String beanNameWithPackage, List<FieldDescription> beanFields) {
        fixDuplicates(beanFields);
        this.fields = beanFields;
        this.beanNameWithPackage = beanNameWithPackage;
        this.beanType = Type.getType(ByteCodeUtils.toTypeDescriptor(beanNameWithPackage));
        this.setMethod = Method.getMethod(beanNameWithPackage + " set(org.openl.rules.calc.SpreadsheetResult, String, String, Class, java.util.function.BiFunction)");
    }

    private byte[] getBytes() {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        visitClassDescription(classWriter);
        visitClassAnnotations(classWriter);
        visitConstructor(classWriter);
        visitFields(classWriter);
        visitValueOf(classWriter);
        return classWriter.toByteArray();
    }

    private static void fixDuplicates(List<FieldDescription> fields) {
        var names = new HashSet<String>(fields.size());
        var duplicates = new ArrayList<FieldDescription>();
        for (var field : fields) {
            if (!names.add(field.fieldName)) {
                duplicates.add(field);
            }
        }
        for (var field : duplicates) {
            var name = field.fieldName;
            int i = 1;
            while (names.contains(name + i)) {
                i++;
            }
            field.fieldName = name + i;
            names.add(field.fieldName);
        }
    }

    private void visitClassDescription(ClassWriter classWriter) {
        classWriter.visit(Opcodes.V11,
                Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                beanType.getInternalName(),
                null,
                "java/lang/Object",
                new String[]{"java/io/Serializable"});
    }

    private void visitClassAnnotations(ClassWriter classWriter) {
        var parts = StringUtils.split(beanNameWithPackage, '.');

        var str = new StringBuilder(beanNameWithPackage.length());
        str.append("https://");
        for (int i = parts.length - 2; i >= 0; i--) {
            str.append(parts[i]).append('.');
            if (i != 0) {
                str.append('.');
            }
        }
        String namespace = str.toString();
        String name = parts[parts.length - 1];
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
        var map = classWriter.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "values", "Ljava/util/HashMap;", "Ljava/util/HashMap<Lorg/openl/cache/GenericKey;Ljava/lang/Object;>;", null);
        map.visitEnd();

        var mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC, DEFAULT_CONSTRUCTOR, null, null, classWriter);

        // invokes the super class constructor
        mg.loadThis();
        mg.invokeConstructor(Type.getType(Object.class), DEFAULT_CONSTRUCTOR);

        mg.loadThis();
        mg.newInstance(VALUES_TYPE);
        mg.dup();
        mg.push(fields.size());
        mg.push(1.0f);
        mg.invokeConstructor(VALUES_TYPE, Method.getMethod("void <init> (int, float)"));
        mg.putField(beanType, "values", VALUES_TYPE);

        mg.returnValue();
        mg.endMethod();
    }

    private void visitFields(ClassWriter classWriter) {
        for (var field : fields) {
            generateGetter(classWriter, field.fieldName, field);
            generateSetter(classWriter, field.fieldName, field);
        }
    }

    private void generateGetter(ClassWriter classWriter, String fieldName, FieldDescription fieldDescription) {
        String getterMethod = fieldDescription.className + " " + ClassUtils.getter(fieldName) + "()";
        var mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC, Method.getMethod(getterMethod), null, null, classWriter);
        mg.loadThis();
        mg.getField(beanType, "values", VALUES_TYPE);
        mg.push(fieldDescription.row);
        mg.push(fieldDescription.column);
        mg.invokeStatic(GENERIC_KEY_TYPE, GENERIC_KEY_CREATOR);
        mg.invokeVirtual(VALUES_TYPE, GET_VALUE);
        mg.checkCast(fieldDescription.type);
        mg.returnValue();

        var av = mg.visitAnnotation(SPREADSHEET_CELL, true);
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
        mg.endMethod();
    }

    private void generateSetter(ClassWriter classWriter, String fieldName, FieldDescription fieldDescription) {
        String setterMethod = "void " + ClassUtils.setter(fieldName) + "(" + fieldDescription.className + ")";
        var mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC, Method.getMethod(setterMethod), null, null, classWriter);

        mg.loadThis();
        mg.getField(beanType, "values", VALUES_TYPE);
        mg.push(fieldDescription.row);
        mg.push(fieldDescription.column);
        mg.invokeStatic(GENERIC_KEY_TYPE, GENERIC_KEY_CREATOR);
        mg.loadArg(0);
        mg.invokeVirtual(VALUES_TYPE, SET_VALUE);
        mg.pop();

        mg.returnValue();
        mg.endMethod();
    }

    private void visitValueOf(ClassWriter classWriter) {
        visitSet(classWriter);

        var valueOf = Method.getMethod(beanNameWithPackage + " valueOf(org.openl.rules.calc.SpreadsheetResult, java.util.function.BiFunction)");
        var mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, valueOf, null, null, classWriter);

        // {
        mg.visitCode();

        // bean = new Bean();
        mg.newInstance(beanType);
        mg.dup();
        mg.invokeConstructor(beanType, DEFAULT_CONSTRUCTOR);

        for (var field : fields) {
            // bean = bean.set(sr, row, column, clazz, converter)
            mg.loadArg(0); // sr
            mg.push(field.row);
            mg.push(field.column);
            mg.push(field.type);
            mg.loadArg(1); // converter
            mg.invokeVirtual(beanType, setMethod);
        }

        // return bean;
        mg.returnValue();

        // }
        mg.endMethod();
    }

    private void visitSet(ClassWriter classWriter) {

        var mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE, setMethod, null, null, classWriter);

        // {
        mg.visitCode();

        // _a = this.values;
        mg.loadThis();
        mg.getField(beanType, "values", VALUES_TYPE);

        // _k = GenericKey.geyInstance(row, column)
        mg.loadArg(1); // row
        mg.loadArg(2); // column
        mg.invokeStatic(GENERIC_KEY_TYPE, GENERIC_KEY_CREATOR);

        // put on the stack
        mg.loadArg(4); // converter

        // _v = sr.getValue(row, column);
        mg.loadArg(0); // sr
        mg.loadArg(1); // row
        mg.loadArg(2); // column
        mg.invokeVirtual(SR_TYPE, SR_GET_VALUE);

        // _v = converter.apply(_v, clazz)
        mg.loadArg(3); // clazz
        mg.invokeInterface(Type.getType(BiFunction.class), Method.getMethod("Object apply(Object, Object)"));

        // _z = _a.put(_k, _v);
        mg.invokeVirtual(VALUES_TYPE, SET_VALUE);

        // remove _z from the stack
        mg.pop();

        // return this;
        mg.loadThis();
        mg.returnValue();

        // }
        mg.endMethod();
    }

    private static String toJavaIdentifier(String s) {
        if (!s.isEmpty()) {
            s = s.replaceAll("\\s+", "_"); // Replace whitespaces
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                if (Character.isJavaIdentifierPart(s.charAt(i))) {
                    sb.append(s.charAt(i));
                }
            }
            s = sb.toString();
            if (JavaKeywordUtils.isJavaKeyword(s) || !s.isEmpty() && !Character.isJavaIdentifierStart(s.charAt(0))) {
                s = "_" + s;
            }
        }
        return s;
    }

    static final class FieldDescription {
        final String className;
        final Type type;
        final String row;
        final String column;
        final boolean FIX_ME;
        String fieldName;

        FieldDescription(String canonicalClassName, String row, String column, boolean FIX_ME) {
            this.className = canonicalClassName;
            this.type = Type.getType(ByteCodeUtils.toTypeDescriptor(canonicalClassName));
            this.row = row;
            this.column = column;
            this.FIX_ME = FIX_ME;
            String fieldName;
            if (column == null) {
                fieldName = toJavaIdentifier(row);
            } else if (row == null) {
                fieldName = toJavaIdentifier(column);
            } else {
                fieldName = toJavaIdentifier(column) + StringUtils.capitalize(toJavaIdentifier(row));
            }
            if (fieldName.isEmpty()) {
                fieldName = "_";
            }
            this.fieldName = ClassUtils.decapitalize(fieldName);
        }
    }
}
