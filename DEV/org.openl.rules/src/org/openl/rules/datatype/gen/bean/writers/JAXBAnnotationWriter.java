package org.openl.rules.datatype.gen.bean.writers;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

/**
 * Writer that supports writing annotation to class in byte code.
 * 
 * @author Marat Kamalov
 *
 */
public class JAXBAnnotationWriter implements BeanByteCodeWriter { 

    String beannameWithPackage;
    
    public JAXBAnnotationWriter(String beanNameWithPackage) {
        this.beannameWithPackage = beanNameWithPackage;
    }

    @Override
    public void write(ClassWriter classWriter) {
        String[] parts = beannameWithPackage.split("/");
        StringBuilder namespace = new StringBuilder("http://"); 
        for (int i = parts.length - 2; i >= 0; i--) {
            namespace.append(parts[i]);
            if (i != 0) {
                namespace.append(".");
            }
        }
        
        AnnotationVisitor av = classWriter.visitAnnotation(Type.getDescriptor(XmlRootElement.class), true);
        av.visit("namespace", namespace.toString());
        av.visitEnd();

        AnnotationVisitor av1 = classWriter.visitAnnotation(Type.getDescriptor(XmlType.class), true);
        av1.visit("namespace", namespace.toString());
        av1.visitEnd();

    }
}
