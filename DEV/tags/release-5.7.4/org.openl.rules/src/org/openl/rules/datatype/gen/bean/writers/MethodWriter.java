package org.openl.rules.datatype.gen.bean.writers;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.CodeVisitor;
import org.objectweb.asm.Constants;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;

public abstract class MethodWriter implements BeanByteCodeWriter {
    
    private String beanNameWithPackage;
    private Map<String, FieldDescription> allFields;
    
    /**
     * Number of fields that will take 2 stack elements(like a double and long)
     */
    private int twoStackElementFieldsCount;
    
    public MethodWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        this.beanNameWithPackage = beanNameWithPackage;
        this.allFields = new HashMap<String, FieldDescription>(allFields);
        this.twoStackElementFieldsCount = ByteCodeGeneratorHelper.getTwoStackElementFieldsCount(allFields);
    }

    protected String getBeanNameWithPackage() {
        return beanNameWithPackage;
    }

    protected Map<String, FieldDescription> getAllFields() {
        return allFields;
    }

    protected int getTwoStackElementFieldsCount() {
        return twoStackElementFieldsCount;
    }
    
    protected void pushFieldToStack(CodeVisitor codeVisitor, int fieldOwnerLocalVarIndex, String fieldName) {
        codeVisitor.visitVarInsn(Constants.ALOAD, 0);
        codeVisitor.visitFieldInsn(Constants.GETFIELD, getBeanNameWithPackage(), fieldName, ByteCodeGeneratorHelper.getJavaType(getAllFields()
                .get(fieldName)));
    }
    
}
