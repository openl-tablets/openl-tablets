package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.xml.bind.annotation.XmlElement;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
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
    public static final Type VALUES_TYPE = Type.getType(HashMap.class);
    public static final Method GET_VALUE = Method.getMethod("Object get(Object)");
    public static final Method SET_VALUE = Method.getMethod("Object put(Object, Object)");
    private static final Type SR_TYPE = Type.getType(SpreadsheetResult.class);
    private static final Method SR_GET_VALUE = Method.getMethod("Object getModelValue(String)");
    private static final Type METHOD_TYPE = Type.getType(java.lang.reflect.Method.class);

    private final List<FieldDescription> fields;
    private final Type beanType;
    private final String namespace;
    private final String name;
    private final String propertyNameSpace;
    private final Method valueOfMethod;


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
        fixDuplicates(beanFields, (field, name) -> field.fieldName = name, field -> field.fieldName);
        fixDuplicates(beanFields, (field, name) -> field.xmlName = name, field -> field.xmlName);
        this.fields = beanFields;
        this.beanType = Type.getType(ByteCodeUtils.toTypeDescriptor(beanNameWithPackage));
        this.valueOfMethod = Method.getMethod(beanNameWithPackage + " valueOf(org.openl.rules.calc.SpreadsheetResult, java.util.function.BiFunction)");

        var parts = StringUtils.split(beanNameWithPackage, '.');

        var str = new StringBuilder(beanNameWithPackage.length());
        str.append("https://");
        for (int i = parts.length - 2; i >= 0; i--) {
            str.append(parts[i]);
            if (i != 0) {
                str.append('.');
            }
        }
        this.namespace = str.toString();
        this.name = parts[parts.length - 1];
        this.propertyNameSpace = this.namespace + "/" + name;
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
        classWriter.visit(Opcodes.V11,
                Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                beanType.getInternalName(),
                null,
                "java/lang/Object",
                new String[]{"java/io/Serializable"});
    }

    private void visitClassAnnotations(ClassWriter classWriter) {
        var av = classWriter.visitAnnotation("Ljakarta/xml/bind/annotation/XmlRootElement;", true);
        av.visit("namespace", namespace);
        av.visit("name", name);
        av.visitEnd();

        av = classWriter.visitAnnotation("Ljakarta/xml/bind/annotation/XmlAccessorType;", true);
        av.visitEnum("value", "Ljakarta/xml/bind/annotation/XmlAccessType;", "PROPERTY");
        av.visitEnd();

        av = classWriter.visitAnnotation(SR_BEAN_CLASS, true);
        av.visitEnd();

        av = classWriter.visitAnnotation("Ljakarta/xml/bind/annotation/XmlType;", true);
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
        mg.push(fieldDescription.cell);
        mg.invokeVirtual(VALUES_TYPE, GET_VALUE);
        mg.checkCast(fieldDescription.type);
        mg.returnValue();

        var av = mg.visitAnnotation(SPREADSHEET_CELL, true);
        av.visit("cell", fieldDescription.cell);
        if (fieldDescription.column != null) {
            av.visit("column", fieldDescription.column);

        }
        if (fieldDescription.row != null) {
            av.visit("row", fieldDescription.row);
        }
        av.visitEnd();

        av = mg.visitAnnotation(XML_ELEMENT, true);
        av.visit("name", fieldDescription.xmlName);
        // EPBDS-9437 OpenAPI schema test is failing without propertyNameSpace.
        // Driver_Forms property is disappeared from the AnySpreadsheetResult component.
        // It happens because of io.swagger.v3.core.converter.AnnotatedType.equals returns true for the different properties
        // having the same type (AnySpreadsheetResult) and the same set of the annotations.
        // So io.swagger.v3.core.converter.ModelConverterContextImpl.processedTypes contains no fully processed type.
        // See also InheritanceFixConverter class.
        av.visit("namespace", propertyNameSpace);
        av.visitEnd();

        mg.endMethod();
    }

    private void generateSetter(ClassWriter classWriter, String fieldName, FieldDescription fieldDescription) {
        String setterMethod = "void " + ClassUtils.setter(fieldName) + "(" + fieldDescription.className + ")";
        var mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC, Method.getMethod(setterMethod), null, null, classWriter);

        mg.loadThis();
        mg.getField(beanType, "values", VALUES_TYPE);
        mg.push(fieldDescription.cell);
        mg.loadArg(0);
        mg.invokeVirtual(VALUES_TYPE, SET_VALUE);
        mg.pop();

        mg.returnValue();
        mg.endMethod();
    }

    private void visitValueOf(ClassWriter classWriter) {
        var mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, valueOfMethod, null, null, classWriter);

        // {
        mg.visitCode();

        // bean = new Bean();
        mg.newInstance(beanType);
        mg.dup();
        mg.invokeConstructor(beanType, DEFAULT_CONSTRUCTOR);
        var bean = mg.newLocal(beanType);
        mg.storeLocal(bean);

        // array
        mg.push(beanType);
        mg.invokeVirtual(Type.getType(Class.class), Method.getMethod("java.lang.reflect.Method[] getDeclaredMethods()"));
        var array = mg.newLocal(Type.getType(java.lang.reflect.Method[].class));
        mg.storeLocal(array);

        // end index
        mg.loadLocal(array);
        mg.arrayLength();
        var end = mg.newLocal(Type.INT_TYPE);
        mg.storeLocal(end);

        // index
        mg.push(0);
        var index = mg.newLocal(Type.INT_TYPE);
        mg.storeLocal(index);


        var methodVisitor = mg.getDelegate();

        var forLoop = mg.mark();
        methodVisitor.visitFrame(Opcodes.F_FULL, 6, new Object[]{"org/openl/rules/calc/SpreadsheetResult", "java/util/function/BiFunction", beanType.getInternalName(), "[Ljava/lang/reflect/Method;", Opcodes.INTEGER, Opcodes.INTEGER}, 0, new Object[]{});

        // index >= end
        mg.loadLocal(index);
        mg.loadLocal(end);
        var exitFor = mg.newLabel();
        mg.ifCmp(Type.INT_TYPE, GeneratorAdapter.GE, exitFor);

        // method = array[index]
        mg.loadLocal(array);
        mg.loadLocal(index);
        mg.arrayLoad(METHOD_TYPE);
        var method = mg.newLocal(METHOD_TYPE);
        mg.storeLocal(method);

        // annotation = method.getAnnotation(SpreadsheetCell.class)
        mg.loadLocal(method);
        mg.push(Type.getType(SpreadsheetCell.class));
        mg.invokeVirtual(METHOD_TYPE, Method.getMethod("java.lang.annotation.Annotation getAnnotation(Class)"));
        mg.checkCast(Type.getType(SpreadsheetCell.class));
        var annotation = mg.newLocal(Type.getType(SpreadsheetCell.class));
        mg.storeLocal(annotation);

        // if annotation == null
        mg.loadLocal(annotation);
        var exitIf = mg.newLabel();
        mg.ifNull(exitIf);

        // cell = annotation.getCell()
        mg.loadLocal(annotation);
        mg.invokeInterface(Type.getType(SpreadsheetCell.class), Method.getMethod("String cell()"));
        var cell = mg.newLocal(Type.getType(String.class));
        mg.storeLocal(cell);

        // put on the stack
        mg.loadLocal(bean);
        mg.getField(beanType, "values", VALUES_TYPE);
        mg.loadLocal(cell); // cell
        mg.loadArg(1); // converter

        // _v = sr.getFieldValue(cell);
        mg.loadArg(0); // sr
        mg.loadLocal(cell); // cell
        mg.invokeVirtual(SR_TYPE, SR_GET_VALUE);

        // _v = converter.apply(_v, method.getReturnType())
        mg.loadLocal(method);
        mg.invokeVirtual(METHOD_TYPE, Method.getMethod("Class getReturnType()"));
        mg.invokeInterface(Type.getType(BiFunction.class), Method.getMethod("Object apply(Object, Object)"));

        // _z = values.put(cell, _v);
        mg.invokeVirtual(VALUES_TYPE, SET_VALUE);
        // remove _z from the stack
        mg.pop();

        mg.mark(exitIf);
        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        // index++
        mg.iinc(index, 1);
        mg.goTo(forLoop);

        mg.mark(exitFor);
        methodVisitor.visitFrame(Opcodes.F_CHOP, 3, null, 0, null);

        // return bean
        mg.loadLocal(bean);
        mg.returnValue();
        mg.endMethod();

    }

    static final class FieldDescription {
        final String className;
        final Type type;
        final String row;
        final String column;
        final String cell;
        String fieldName;
        String xmlName;

        FieldDescription(String canonicalClassName, String row, String column) {
            this.className = canonicalClassName;
            this.type = Type.getType(ByteCodeUtils.toTypeDescriptor(canonicalClassName));
            this.row = row;
            this.column = column;
            this.cell = ASpreadsheetField.createFieldName(column, row);
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
