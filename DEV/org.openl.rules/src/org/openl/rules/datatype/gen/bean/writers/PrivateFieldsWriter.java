package org.openl.rules.datatype.gen.bean.writers;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
public class PrivateFieldsWriter extends DefaultBeanByteCodeWriter {
    
    /**
     * 
     * @param beanFields fields of generating class.
     */
    public PrivateFieldsWriter(Map<String, FieldDescription> beanFields) {
        super(StringUtils.EMPTY, null, beanFields);        
    }
    
    public void write(ClassWriter classWriter) {
        for (Map.Entry<String,  FieldDescription> field : getBeanFields().entrySet()) {
          String fieldTypeName = ByteCodeGeneratorHelper.getJavaType(field.getValue());          
          classWriter.visitField(Opcodes.ACC_PROTECTED, field.getKey(), fieldTypeName, null, null);
        }
    }

}
