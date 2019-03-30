package org.openl.gen.writers;

import java.util.Map;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.gen.FieldDescription;

public abstract class MethodWriter extends DefaultBeanByteCodeWriter {

    public static final String VOID_CLASS_NAME = "void";

    public MethodWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, null, allFields);
    }

    protected Map<String, FieldDescription> getAllFields() {
        return getBeanFields();
    }

    protected void pushFieldToStack(MethodVisitor codeVisitor, int fieldOwnerLocalVarIndex, String fieldName) {
        codeVisitor.visitVarInsn(Opcodes.ALOAD, fieldOwnerLocalVarIndex);
        codeVisitor.visitFieldInsn(Opcodes.GETFIELD,
            getBeanNameWithPackage(),
            fieldName,
            getAllFields().get(fieldName).getTypeDescriptor());
    }

    public static boolean containRestrictedSymbols(String fieldName) {
        /**
         * regex for validating field names. Field name may start from '_', any letter or '$' sign. And may be followed
         * by the described symbols and also by any number.
         */
        String regex = "^(_|[a-zA-Z]|\\$)(_|[a-zA-Z0-9]|\\$)*";

        return !fieldName.matches(regex);
    }

    /**
     * Generate methods only for fields without restricted symbols. In future should be updated to use this fields too
     * somehow
     */
    protected boolean validField(String fieldName, FieldDescription fieldDescription) {
        return !fieldDescription.getTypeName().equals(VOID_CLASS_NAME) && !containRestrictedSymbols(fieldName);
    }

}
