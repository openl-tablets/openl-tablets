package org.openl.util.generation;

import org.openl.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

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
        StringBuilder buf = new StringBuilder(10000);

        addComment(buf);

        addPackage(buf);

        addImports(buf);

        addClassDeclaration(buf, ClassUtils.getShortClassName(getClassForGeneration()),
                ClassUtils.getShortClassName(getClassForGeneration().getSuperclass()));

        buf.append(JavaClassGeneratorHelper.getUUID());

        addConstructors(buf);

        addMethods(buf);

        buf.append("\n}");

        return buf.toString();
    }

    private void addMethods(StringBuilder buf) {
        for (Method method : getClassForGeneration().getDeclaredMethods()) {
            if (method.getName().startsWith(JavaGenerator.GET)) {
                addDecoratorGetter(buf, method);
            }
        }
    }

    private void addDecoratorGetter(StringBuilder buf, Method method) {
        String fieldName = getFieldName(method.getName());
        buf.append(JavaClassGeneratorHelper.getGetterWithCastMethod(method.getReturnType(), SPREADSHEET_METHOD,
                fieldName));
    }

    public String getFieldName(String methodName) {
        /** skip first 3 letters ('get') */
        return methodName.substring(3);
    }

    private void addConstructors(StringBuilder buf) {
        for (Constructor<?> constructor : getClassForGeneration().getSuperclass().getConstructors()) {
            Map<String, Class<?>> superClassFields = new LinkedHashMap<String, Class<?>>();
            int i = 1;
            for (Class<?> superClassFieldType : constructor.getParameterTypes()) {
                superClassFields.put("par" + i, superClassFieldType);
                i++;
            }

            String constructorStr = JavaClassGeneratorHelper.getConstructorWithFields(getClassForGeneration()
                    .getSimpleName(), superClassFields, constructor.getParameterTypes().length);
            buf.append(constructorStr);
        }
    }

}
