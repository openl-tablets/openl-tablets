package org.openl.gen.groovy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import org.openl.gen.AnnotationDescription;

public class SimpleGroovyScriptGenerator {

    private final String packageName;
    private final String simpleClassName;

    public SimpleGroovyScriptGenerator(String beanFullName) {
        int lastDot = beanFullName.lastIndexOf(".");
        String[] dividedName = {beanFullName.substring(0, lastDot), beanFullName.substring(lastDot + 1)};
        this.packageName = dividedName[0];
        this.simpleClassName = dividedName[1];
    }

    public String getSimpleClassName() {
        return simpleClassName;
    }

    public String scriptText() {
        StringBuilder sb = new StringBuilder();
        sb.append("package")
                .append(" ")
                .append(packageName)
                .append(GroovyMethodWriter.LINE_SEPARATOR)
                .append(GroovyMethodWriter.LINE_SEPARATOR);

        sb.append(generateImports());
        sb.append(generateJAXBAnnotations());
        sb.append(generateClassDescription());
        sb.append(" ").append("{").append(GroovyMethodWriter.LINE_SEPARATOR).append(GroovyMethodWriter.LINE_SEPARATOR);

        sb.append(generateExtraMethods());

        sb.append("}");
        return sb.toString();
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
                new AnnotationDescription.AnnotationProperty[]{
                        new AnnotationDescription.AnnotationProperty("namespace", namespace),
                        new AnnotationDescription.AnnotationProperty("name", simpleClassName)});

        result.append(
                AnnotationTransformationHelper.transformAnnotation(xmlRootElemDescription, null, getDefaultImports()));
        result.append(GroovyMethodWriter.LINE_SEPARATOR);

        AnnotationDescription xmlAccessorType = new AnnotationDescription(XmlAccessorType.class,
                new AnnotationDescription.AnnotationProperty[]{
                        new AnnotationDescription.AnnotationProperty("value", XmlAccessType.FIELD)});

        result.append(AnnotationTransformationHelper.transformAnnotation(xmlAccessorType, null, getDefaultImports()));
        result.append(GroovyMethodWriter.LINE_SEPARATOR);

        AnnotationDescription xmlType = new AnnotationDescription(XmlType.class,
                new AnnotationDescription.AnnotationProperty[]{
                        new AnnotationDescription.AnnotationProperty("namespace", namespace),
                        new AnnotationDescription.AnnotationProperty("name", simpleClassName)});

        result.append(AnnotationTransformationHelper.transformAnnotation(xmlType, null, getDefaultImports()));
        result.append(GroovyMethodWriter.LINE_SEPARATOR);
        return result.toString();
    }

    protected String generateExtraMethods() {
        return "";
    }

    protected String[] getDefaultInterfaces() {
        return new String[]{Serializable.class.getName()};
    }

    protected Set<String> getDefaultImports() {
        return new HashSet<>(Arrays.asList(Method.class.getName(),
                XmlAccessorType.class.getName(),
                XmlAccessType.class.getName(),
                XmlType.class.getName(),
                XmlRootElement.class.getName()));
    }

    protected String generateClassDescription() {
        return "class " + getSimpleClassName();
    }

    protected String generateImports() {
        return "";
    }

}