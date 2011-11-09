package org.openl.util.generation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;

/**
 * Java class generator for Custom Spreadsheet Results.
 * 
 * Currently is not used anywhere. Don`t delete it. 
 * 
 * @author DLiauchuk
 *
 */
public class CustomSpreadsheetResultJavaGenerator extends JavaGenerator {
    
    public static final String SPREADSHEET_METHOD = "getFieldValue";
    
    public CustomSpreadsheetResultJavaGenerator(Class<?> customSpreadsheetResultClass) {
        super(customSpreadsheetResultClass);
    }
    
    public String generateJavaClass() {        
        StringBuffer buf = new StringBuffer(10000);
        
        addComment(buf);
        
        addPackage(buf);
        
        addImports(buf);
        
        addClassDeclaration(buf, ClassUtils.getShortClassName(getClassNameForGeneration()), ClassUtils.getShortClassName(getClassForGeneration().getSuperclass()));
        
        buf.append(JavaClassGeneratorHelper.getUUID());
        
        addConstructors(buf);
        
        addMethods(buf);
        
        buf.append("\n}");
        
        return buf.toString();
    }
    
    private void addMethods(StringBuffer buf) {        
        for (Method method : getClassForGeneration().getDeclaredMethods()) {
            if (method.getName().startsWith(JavaGenerator.GET)) {
                addDecoratorGetter(buf, method);
            }
        }
    }
    
    private void addDecoratorGetter(StringBuffer buf, Method method) {
        String fieldName = getFieldName(method.getName());        
        buf.append(JavaClassGeneratorHelper.getGetterWithCastMethod(method.getReturnType(), SPREADSHEET_METHOD,  fieldName));
    }
        
    private void addConstructors(StringBuffer buf) {
        for (Constructor<?> constructor : getClassForGeneration().getSuperclass().getConstructors()) {
            Map<String, Class<?>> superClassFields = new LinkedHashMap<String, Class<?>>();
            int i = 1;
            for (Class<?> superClassFieldType : constructor.getParameterTypes()) {
                superClassFields.put("par" + i, superClassFieldType);
                i++;
            }
            
            String constructorStr = JavaClassGeneratorHelper.getConstructorWithFields(getClassForGeneration().getSimpleName(), superClassFields, constructor.getParameterTypes().length);
            buf.append(constructorStr);            
        }
    } 

}
