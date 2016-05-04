package org.openl.conf.ant;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.util.StringUtils;
import org.openl.util.generation.JavaClassGeneratorHelper;

public class JavaInterfaceGenerator implements OpenLToJavaGenerator {

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

    @Override
    public String generateJava() {
        StringBuilder buf = new StringBuilder(1000);

        // Add comment
        buf.append(JavaClassGeneratorHelper.getCommentText("This class has been generated."));

        // Add packages
        buf.append(JavaClassGeneratorHelper.getPackageText(targetPackageName));

        // Add interface declaration
        buf.append(JavaClassGeneratorHelper.getInterfaceDeclaration(targetClassName));

        buf.append(" {\n");

        addFieldMethods(buf);

        // Add methods
        addMethods(buf);

        buf.append("}");

        return buf.toString();
    }

    private void addMethods(StringBuilder buf) {
        for (IOpenMethod method : moduleOpenClass.getMethods()) {
            if (!JavaWrapperGenerator.shouldBeGenerated(method, methodsToGenerate, moduleOpenClass.getName(),
                    ignoreNonJavaTypes, ignoreTestMethods)) {
                continue;
            }
            buf.append("  ");
            JavaWrapperGenerator.addMethodName(method, buf);
            buf.append(";\n\n");
        }
    }

    private void addFieldMethods(StringBuilder buf) {
        for (IOpenField field : moduleOpenClass.getFields().values()) {
            if (!JavaWrapperGenerator.shouldBeGenerated(field, fieldsToGenerate, ignoreNonJavaTypes, ignoreTestMethods)) {
                continue;
            }
            addFieldAccessor(field, buf);
        }
    }

    private void addFieldAccessor(IOpenField field, StringBuilder buf) {

        IOpenClass type = field.getType();

        String className = JavaWrapperGenerator.getClassName(type.getInstanceClass());

        JavaWrapperGenerator.addSignature(field, buf, className);

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

}
