package org.openl.rules.serialization;

/*-
 * #%L
 * OpenL - STUDIO - Jackson
 * %%
 * Copyright (C) 2016 - 2020 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.lang.reflect.Field;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.table.Point;
import org.openl.types.IOpenField;

import com.fasterxml.jackson.annotation.JsonInclude;

class SpreadsheetResultBeanClassMixInAnnotationsWriter extends ClassVisitor {
    private final String className;
    private final Class<?> originalMixInClass;
    private final CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass;
    private final SpreadsheetResultFieldNameResolver spreadsheetResultFieldNameResolver;

    public SpreadsheetResultBeanClassMixInAnnotationsWriter(ClassVisitor delegatedClassVisitor,
            String className,
            Class<?> originalMixInClass,
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass,
            SpreadsheetResultFieldNameResolver spreadsheetResultFieldNameProcessor) {
        super(Opcodes.ASM5, delegatedClassVisitor);
        this.className = className;
        this.originalMixInClass = originalMixInClass;
        this.customSpreadsheetResultOpenClass = customSpreadsheetResultOpenClass;
        this.spreadsheetResultFieldNameResolver = spreadsheetResultFieldNameProcessor;
    }

    @Override
    public void visit(int arg0, int arg1, String arg2, String arg3, String arg4, String[] arg5) {
        super.visit(arg0, arg1, className.replace('.', '/'), arg3, arg4, arg5);
        if (!originalMixInClass.isAnnotationPresent(JsonInclude.class)) {
            AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(JsonInclude.class), true);
            av.visitEnum("value", Type.getDescriptor(JsonInclude.Include.class), JsonInclude.Include.NON_NULL.name());
            av.visitEnd();
        }
        if (spreadsheetResultFieldNameResolver != null) {
            Class<?> sprBeanClass = customSpreadsheetResultOpenClass.getBeanClass();
            BidiMap<String, String> xmlNamesBidiMap = new DualHashBidiMap<>(
                customSpreadsheetResultOpenClass.getXmlNamesMap());
            for (Field field : sprBeanClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(XmlElement.class)) {
                    XmlElement xmlElement = field.getAnnotation(XmlElement.class);
                    FieldVisitor fv = visitField(field
                        .getModifiers(), field.getName(), Type.getDescriptor(field.getType()), null, null);
                    AnnotationVisitor av = fv.visitAnnotation(Type.getDescriptor(XmlElement.class), true);
                    String fieldName = xmlNamesBidiMap.getKey(xmlElement.name());
                    List<IOpenField> fields = customSpreadsheetResultOpenClass.getBeanFieldsMap().get(fieldName);
                    if (fields != null) {
                        String cellName = fields.iterator().next().getName();
                        Point p = customSpreadsheetResultOpenClass.getFieldsCoordinates().get(cellName);
                        av.visit("name",
                            spreadsheetResultFieldNameResolver.resolveName(xmlElement.name(),
                                customSpreadsheetResultOpenClass.getColumnNames()[p.getColumn()],
                                customSpreadsheetResultOpenClass.getRowNames()[p.getRow()]));
                    } else {
                        av.visit("name", spreadsheetResultFieldNameResolver.resolveName(xmlElement.name(), null, null));
                    }
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
}
