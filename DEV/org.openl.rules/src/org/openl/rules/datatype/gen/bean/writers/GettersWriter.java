package org.openl.rules.datatype.gen.bean.writers;

import java.util.Date;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;

/**
 * Writes getters to the generated bean class.
 * 
 * @author DLiauchuk 
 */
public class GettersWriter extends MethodWriter {
    
    /**
     * 
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br> 
     * (e.g. <code>my/test/TestClass</code>)
     * @param beanFields fields of generating class.
     */
    public GettersWriter(String beanNameWithPackage, Map<String, FieldDescription> beanFields) {
        super(beanNameWithPackage, beanFields);
    }
    
    public void write(ClassWriter classWriter) {
        /** ignore those fields that are of void type. In java it is impossible
        but possible in Openl, e.g. spreadsheet cell with void type.*/
        for(Map.Entry<String, FieldDescription> field : getAllFields().entrySet()) {
            if (validField(field.getKey(), field.getValue())) {
                generateGetter(classWriter, field);
            }
        }
    }
    
    /**
     * Generates getter for the fieldEntry.
     * 
     * @param classWriter
     * @param fieldEntry
     */
    protected void generateGetter(ClassWriter classWriter, Map.Entry<String, FieldDescription> fieldEntry) {
        MethodVisitor methodVisitor;
        String fieldName = fieldEntry.getKey();
        FieldDescription field = fieldEntry.getValue();
        String getterName = StringTool.getGetterName(fieldName);

        final String javaType = field.getTypeDescriptor();
        final String format = new StringBuilder(64).append("()").append(javaType).toString();
        methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC,  getterName, format, null, null);
        
        
        String elementName = fieldName;
        if (elementName.length() == 1){
            elementName = elementName.toLowerCase();
        }else{
            if (fieldName.length() > 1 && Character.isUpperCase(elementName.charAt(1))){
                elementName = StringUtils.capitalize(elementName);
            }else{
                elementName = StringUtils.uncapitalize(elementName);
            }
        }
        
        AnnotationVisitor av = methodVisitor.visitAnnotation(Type.getDescriptor(XmlElement.class), true);
        av.visit("name", elementName);
        
        if (fieldEntry.getValue().hasDefaultValue()){
            String defaultFieldValue = fieldEntry.getValue().getDefaultValueAsString();
            if (Boolean.class.getName().equals(fieldEntry.getValue().getTypeName()) || boolean.class.getName().equals(fieldEntry.getValue().getTypeName())){
                defaultFieldValue = String.valueOf(fieldEntry.getValue().getDefaultValue());
            }
            if (fieldEntry.getValue().getTypeName().equals(Date.class.getName())){
                Date date = (Date) fieldEntry.getValue().getDefaultValue();
                defaultFieldValue = ISO8601DateFormater.format(date);
            }
            if (DefaultValue.DEFAULT.equals(defaultFieldValue)){
                av.visit("nillable", false);
            }else{
                av.visit("defaultValue", defaultFieldValue);
            }
        }else{
            av.visit("nillable", true);    
        }
        av.visitEnd();
        
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, getBeanNameWithPackage(), fieldName, javaType);
        String retClass = field.getTypeDescriptor();
        Type type = Type.getType(retClass);
        methodVisitor.visitInsn(type.getOpcode(Opcodes.IRETURN));
        methodVisitor.visitMaxs(0, 0);
    }

}
