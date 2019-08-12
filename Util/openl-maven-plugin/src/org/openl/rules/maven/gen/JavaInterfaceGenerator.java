package org.openl.rules.maven.gen;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.data.DataOpenField;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ArrayTool;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;

@Deprecated
public class JavaInterfaceGenerator {

    private IOpenClass moduleOpenClass;
    private String targetClassName;
    private String targetPackageName;

    private String[] methodsToGenerate;
    private String[] fieldsToGenerate;

    private boolean ignoreNonJavaTypes;
    private boolean ignoreTestMethods;

    private JavaInterfaceGenerator(Builder builder) {
        this.moduleOpenClass = builder.moduleOpenClass;
        this.targetClassName = builder.targetClassName;
        this.targetPackageName = builder.targetPackageName;
        this.methodsToGenerate = builder.methodsToGenerate;
        this.fieldsToGenerate = builder.fieldsToGenerate;
        this.ignoreNonJavaTypes = builder.ignoreNonJavaTypes;
        this.ignoreTestMethods = builder.ignoreTestMethods;
    }

    public String generateJava() {
        StringBuilder buf = new StringBuilder(1000);

        // Add comment
        buf.append(String.format("/*\n * %s \n*/\n\n", "This class has been generated."));

        // Add packages

        if (targetPackageName != null) {
            buf.append(String.format("package %s;\n\n", targetPackageName));
        }

        // Add interface declaration
        buf.append(String.format("\npublic interface %s", targetClassName));

        buf.append(" {\n");

        addFieldMethods(buf);

        // Add methods
        addMethods(buf);

        buf.append("}");

        return buf.toString();
    }

    private void addMethods(StringBuilder buf) {
        for (IOpenMethod method : moduleOpenClass.getMethods()) {
            if (!shouldBeGenerated(method,
                methodsToGenerate,
                moduleOpenClass.getName(),
                ignoreNonJavaTypes,
                ignoreTestMethods)) {
                continue;
            }
            buf.append("  ");
            buf.append(getClassName(method.getType().getInstanceClass())).append(' ');
            buf.append(method.getName());
            buf.append('(');
            IOpenClass[] ptypes = method.getSignature().getParameterTypes();
            for (int i = 0; i < ptypes.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                String parameterName = method.getSignature().getParameterName(i);
                buf.append(getClassName(ptypes[i].getInstanceClass()))
                    .append(' ')
                    .append(parameterName == null ? "arg" + i : parameterName);
            }
            buf.append(')');
            buf.append(";\n\n");
        }
    }

    private void addFieldMethods(StringBuilder buf) {
        for (IOpenField field : moduleOpenClass.getFields().values()) {
            if (!shouldBeGenerated(field, fieldsToGenerate, ignoreNonJavaTypes, ignoreTestMethods)) {
                continue;
            }
            addFieldAccessor(field, buf);
        }
    }

    private void addFieldAccessor(IOpenField field, StringBuilder buf) {

        IOpenClass type = field.getType();

        String className = getClassName(type.getInstanceClass());

        String name = field.getName();
        buf.append("\n  public ").append(className).append(" ").append(ClassUtils.getter(name)).append("()");

        buf.append(";\n\n");
    }

    public static class Builder {
        // Required parameters
        private IOpenClass moduleOpenClass;
        private String targetClassName;
        private String targetPackageName;

        // Optional parameters
        private String[] methodsToGenerate;
        private String[] fieldsToGenerate;
        private boolean ignoreNonJavaTypes;
        private boolean ignoreTestMethods;

        public Builder(IOpenClass moduleOpenClass, String targetClass) {
            if (moduleOpenClass == null) {
                throw new IllegalArgumentException("Cannot generate interface for null openl module class");
            }
            if (StringUtils.isEmpty(targetClass)) {
                throw new IllegalArgumentException("Cannot generate interface for empty target class name");
            }
            this.moduleOpenClass = moduleOpenClass;
            parseClassName(targetClass);
        }

        public Builder methodsToGenerate(String[] methodsToGenerate) {
            if (methodsToGenerate != null) {
                this.methodsToGenerate = methodsToGenerate.clone();
            }
            return this;
        }

        public Builder fieldsToGenerate(String[] fieldsToGenerate) {
            if (fieldsToGenerate != null) {
                this.fieldsToGenerate = fieldsToGenerate.clone();
            }
            return this;
        }

        public Builder ignoreNonJavaTypes(boolean ignoreNonJavaTypes) {
            this.ignoreNonJavaTypes = ignoreNonJavaTypes;
            return this;
        }

        public Builder ignoreTestMethods(boolean ignoreTestMethods) {
            this.ignoreTestMethods = ignoreTestMethods;
            return this;
        }

        public JavaInterfaceGenerator build() {
            return new JavaInterfaceGenerator(this);
        }

        private void parseClassName(String targetClass) {
            int idx = targetClass.lastIndexOf('.');
            if (idx < 0) {
                targetClassName = targetClass;
            } else {
                targetPackageName = targetClass.substring(0, idx);
                targetClassName = targetClass.substring(idx + 1, targetClass.length());
            }
        }

    }

    private boolean shouldBeGenerated(IOpenField field,
            String[] fieldToGenerate,
            boolean ignoreNonJavaTypes,
            boolean ignoreTestMethods) {
        if (ignoreTestMethods && field instanceof DataOpenField) {
            ITable table = ((DataOpenField) field).getTable();
            if (table != null) {
                String type = table.getTableSyntaxNode().getType();
                if (type.equals(XlsNodeTypes.XLS_TEST_METHOD.toString()) || type
                    .equals(XlsNodeTypes.XLS_RUN_METHOD.toString())) {
                    return false;
                }
            }
        }
        if (fieldToGenerate != null && !ArrayTool.contains(fieldToGenerate, field.getName())) {
            return false;
        }

        IOpenClass type = field.getType();
        if (ignoreNonJavaTypes && !(type instanceof JavaOpenClass)) {
            return false;
        }

        if ("this".equals(field.getName()) || "top".equals(field.getName())) {
            return false;
        }

        return true;
    }

    private boolean shouldBeGenerated(IOpenMethod method,
            String[] methodsToGenerate,
            String nameOfTheModule,
            boolean ignoreNonJavaTypes,
            boolean ignoreTestMethods) {
        if (ignoreTestMethods && method instanceof TestSuiteMethod) {
            return false;
        }

        // TODO fix a) provide isConstructor() in OpenMethod b) provide better
        // name for XLS modules
        if (nameOfTheModule.equals(method.getName())) {
            // if (moduleOpenClass.getName().equals(method.getName())) {
            return false;
        }

        if ("getOpenClass".equals(method.getName())) {
            return false;
        }

        if (methodsToGenerate != null && !ArrayTool.contains(methodsToGenerate, method.getName())) {
            return false;
        }

        IOpenClass type = method.getType();
        if (ignoreNonJavaTypes && !(type instanceof JavaOpenClass)) {
            return false;
        }

        IOpenClass[] params = method.getSignature().getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            type = params[i];
            if (ignoreNonJavaTypes && !(type instanceof JavaOpenClass)) {
                return false;
            }

        }
        return true;
    }

    private String getClassName(Class<?> instanceClass) {
        StringBuilder buf = new StringBuilder(30);
        while (instanceClass.isArray()) {
            buf.append("[]");
            instanceClass = instanceClass.getComponentType();
        }

        String result;
        /**
         * Filter Custom Spreadsheet results. These classes are dinamically generated on runtime and are children of
         * SpreadsheetResult. For the wrapper use its parent.
         */
        if (ClassUtils.isAssignable(instanceClass, SpreadsheetResult.class)) {
            result = SpreadsheetResult.class.getName();
        } else {
            result = instanceClass.getName();
        }
        buf.insert(0, result);
        return buf.toString();
    }
}
