package org.openl.rules.datatype.gen.bean.writers;

import java.util.Date;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.util.StringUtils;

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
              if (field.getValue().getType().equals(Date.class)){
                  Object value = field.getValue().getDefaultValue();
                  if (value instanceof Date){
                      Date date = (Date) value;
                      String formatedDate = ISO8601DateFormater.format(date);
                      annotationVisitor.visit("value", formatedDate);
                  }
              }else{
                  annotationVisitor.visit("value", field.getValue().getDefaultValueAsString());
              }
              annotationVisitor.visitEnd();
          }
        }
    }

}
