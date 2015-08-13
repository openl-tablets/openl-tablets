package org.openl.rules.datatype.gen.bean.writers;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.util.generation.DefaultValue;

/**
 * Write fields declarations to the generated bean class.
 * 
 * @author DLiauchuk
 *
 */
public class ProtectedFieldsWriter extends DefaultBeanByteCodeWriter {
    
    /**
     * 
     * @param beanFields fields of generating class.
     */
    public ProtectedFieldsWriter(Map<String, FieldDescription> beanFields) {
        super(StringUtils.EMPTY, null, beanFields);        
    }
    
    public void write(ClassWriter classWriter) {
        for (Map.Entry<String,  FieldDescription> field : getBeanFields().entrySet()) {
          String fieldTypeName = ByteCodeGeneratorHelper.getJavaType(field.getValue());          
          FieldVisitor fieldVisitor = classWriter.visitField(Opcodes.ACC_PROTECTED, field.getKey(), fieldTypeName, null, null);
          if (field.getValue().hasDefaultValue()){
              //Requred for java class generation
              AnnotationVisitor annotationVisitor = fieldVisitor.visitAnnotation(Type.getDescriptor(DefaultValue.class), true);
              annotationVisitor.visit("value", field.getValue().getDefaultValueAsString());
              annotationVisitor.visitEnd();
          }
        }
    }

}
