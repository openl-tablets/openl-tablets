package org.openl.rules.datatype.gen.bean.writers;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;

/**
 * Write fields declarations to the generated bean class.
 * 
 * @author DLiauchuk
 *
 */
public class PrivateFieldsWriter implements BeanByteCodeWriter {
    
    private Map<String, FieldDescription> beanFields;
    
    /**
     * 
     * @param beanFields fields of generating class.
     */
    public PrivateFieldsWriter(Map<String, FieldDescription> beanFields) {
        this.beanFields = new HashMap<String, FieldDescription>(beanFields);
    }
    
    public void write(ClassWriter classWriter) {
        for (Map.Entry<String,  FieldDescription> field : beanFields.entrySet()) {
          String fieldTypeName = ByteCodeGeneratorHelper.getJavaType(field.getValue());          
          classWriter.visitField(Opcodes.ACC_PROTECTED, field.getKey(), fieldTypeName, null, null);
        }
    }

}
