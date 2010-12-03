package org.openl.rules.datatype.gen.bean.writers;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Constants;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;

public class PrivateFieldsWriter implements BeanByteCodeWriter {
    
    private Map<String, FieldDescription> beanFields;
    
    public PrivateFieldsWriter(Map<String, FieldDescription> beanFields) {
        this.beanFields = new HashMap<String, FieldDescription>(beanFields);
    }
    
    /**
     * Write fields declarations to the generated bean class.
     * 
     * @param classWriter
     */
    public void write(ClassWriter classWriter) {
        for (Map.Entry<String,  FieldDescription> field : beanFields.entrySet()) {
          String fieldTypeName = ByteCodeGeneratorHelper.getJavaType(field.getValue());          
          classWriter.visitField(Constants.ACC_PROTECTED, field.getKey(), fieldTypeName, null, null);
        }
    }

}
