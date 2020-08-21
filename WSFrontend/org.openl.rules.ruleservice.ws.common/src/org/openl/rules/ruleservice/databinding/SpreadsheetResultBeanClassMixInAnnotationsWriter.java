package org.openl.rules.ruleservice.databinding;

import java.lang.reflect.Field;

import javax.xml.bind.annotation.XmlElement;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.fasterxml.jackson.annotation.JsonInclude;

class SpreadsheetResultBeanClassMixInAnnotationsWriter extends ClassVisitor {
    private final String className;
    private final Class<?> originalMixInClass;
    private final Class<?> sprBeanClass;

    public SpreadsheetResultBeanClassMixInAnnotationsWriter(ClassVisitor delegatedClassVisitor,
            String className,
            Class<?> originalMixInClass,
            Class<?> sprBeanClass) {
        super(Opcodes.ASM5, delegatedClassVisitor);
        this.className = className;
        this.originalMixInClass = originalMixInClass;
        this.sprBeanClass = sprBeanClass;
    }

    @Override
    public void visit(int arg0, int arg1, String arg2, String arg3, String arg4, String[] arg5) {
        super.visit(arg0, arg1, className.replace('.', '/'), arg3, arg4, arg5);
        if (!originalMixInClass.isAnnotationPresent(JsonInclude.class)) {
            AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(JsonInclude.class), true);
            av.visitEnum("value", Type.getDescriptor(JsonInclude.Include.class), JsonInclude.Include.NON_NULL.name());
            av.visitEnd();
        }

        for (Field field : sprBeanClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(XmlElement.class)) {
                XmlElement xmlElement = field.getAnnotation(XmlElement.class);
                FieldVisitor fv = visitField(field
                    .getModifiers(), field.getName(), Type.getDescriptor(field.getType()), null, null);
                AnnotationVisitor av = fv.visitAnnotation(Type.getDescriptor(XmlElement.class), true);
                av.visit("name", xmlElement.name());//.toLowerCase());
                av.visit("nillable", xmlElement.nillable());
                av.visit("required", xmlElement.required());
                av.visit("namespace", xmlElement.namespace());
                av.visit("defaultValue", xmlElement.defaultValue());
                av.visit("type", Type.getType(xmlElement.type()));
                av.visitEnd();
                fv.visitEnd();
            }
        }
    }
}