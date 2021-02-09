package org.openl.gen.groovy;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.openl.gen.AnnotationDescription;

public class SimpleGroovyScriptGenerator {

    private static final String CLASS = "class";

    private final String packageName;
    private final String simpleClassName;

    public SimpleGroovyScriptGenerator(String beanFullName) {
        int lastDot = beanFullName.lastIndexOf(".");
        String[] dividedName = { beanFullName.substring(0, lastDot), beanFullName.substring(lastDot + 1) };
        this.packageName = dividedName[0];
        this.simpleClassName = dividedName[1];
    }

    public String getSimpleClassName() {
        return simpleClassName;
    }

    public String scriptText() {
        StringBuilder scriptText = new StringBuilder("");
        scriptText.append("package")
            .append(" ")
            .append(packageName)
            .append(GroovyMethodWriter.LINE_SEPARATOR)
            .append(GroovyMethodWriter.LINE_SEPARATOR);

        scriptText.append(generateImports());
        scriptText.append(generateJAXBAnnotations());
        scriptText.append(generateClassDescription());
        scriptText.append(" ")
            .append("{")
            .append(GroovyMethodWriter.LINE_SEPARATOR)
            .append(GroovyMethodWriter.LINE_SEPARATOR);

        scriptText.append(generateExtraMethods());

        scriptText.append("}");
        return scriptText.toString();
    }

    protected String generateJAXBAnnotations() {
        StringBuilder result = new StringBuilder();
        StringBuilder namespaceBuilder = new StringBuilder("http://");
        String[] packageNameElements = packageName.split("\\.");
        for (int i = packageNameElements.length - 1; i >= 0; i--) {
            namespaceBuilder.append(packageNameElements[i]);
            if (i != 0) {
                namespaceBuilder.append(".");
            }
        }
        String namespace = namespaceBuilder.toString();

        AnnotationDescription xmlRootElemDescription = new AnnotationDescription(XmlRootElement.class,
            new AnnotationDescription.AnnotationProperty[] {
                    new AnnotationDescription.AnnotationProperty("namespace", namespace),
                    new AnnotationDescription.AnnotationProperty("name", simpleClassName) });

        result.append(
            AnnotationTransformationHelper.transformAnnotation(xmlRootElemDescription, null, getDefaultImports()));
        result.append(GroovyMethodWriter.LINE_SEPARATOR);

        AnnotationDescription xmlAccessorType = new AnnotationDescription(XmlAccessorType.class,
            new AnnotationDescription.AnnotationProperty[] {
                    new AnnotationDescription.AnnotationProperty("value", XmlAccessType.FIELD) });

        result.append(AnnotationTransformationHelper.transformAnnotation(xmlAccessorType, null, getDefaultImports()));
        result.append(GroovyMethodWriter.LINE_SEPARATOR);

        AnnotationDescription xmlType = new AnnotationDescription(XmlType.class,
            new AnnotationDescription.AnnotationProperty[] {
                    new AnnotationDescription.AnnotationProperty("namespace", namespace),
                    new AnnotationDescription.AnnotationProperty("name", simpleClassName) });

        result.append(AnnotationTransformationHelper.transformAnnotation(xmlType, null, getDefaultImports()));
        result.append(GroovyMethodWriter.LINE_SEPARATOR);
        return result.toString();
    }

    protected String generateExtraMethods() {
        return "";
    }

    protected String[] getDefaultInterfaces() {
        return new String[] { "java.io.Serializable" };
    }

    protected Set<String> getDefaultImports() {
        return new HashSet<>(Arrays.asList(Method.class.getName(),
            XmlAccessorType.class.getName(),
            XmlAccessType.class.getName(),
            XmlType.class.getName(),
            XmlRootElement.class.getName()));
    }

    protected String generateClassDescription() {
        return new StringBuilder(CLASS).append(" ").append(getSimpleClassName()).toString();
    }

    protected String generateImports() {
        return "";
    }

}