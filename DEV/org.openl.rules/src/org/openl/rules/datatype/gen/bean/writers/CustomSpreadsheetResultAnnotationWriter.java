package org.openl.rules.datatype.gen.bean.writers;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResultRootDictionaryContext;
import org.openl.types.java.CustomJavaOpenClass;

public class CustomSpreadsheetResultAnnotationWriter implements BeanByteCodeWriter {

    public CustomSpreadsheetResultAnnotationWriter() {
    }

    @Override
    public void write(ClassWriter classWriter) {
        AnnotationVisitor av = classWriter.visitAnnotation(Type.getDescriptor(CustomJavaOpenClass.class), true);
        av.visit("type", Type.getType(CustomSpreadsheetResultOpenClass.class));
        av.visit("variableInContextFinder", Type.getType(SpreadsheetResultRootDictionaryContext.class));
        av.visitEnd();
    }
}