package org.openl.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openl.gen.groovy.GroovyInterfaceScriptGenerator;
import org.openl.util.StringUtils;

/**
 * Java Interface builder
 *
 * @author Vladyslav Pikus
 */
public class InterfaceByteCodeBuilder {

    private final String nameWithPackage;
    private final List<MethodDescription> methods = new ArrayList<>();

    private InterfaceByteCodeBuilder(String nameWithPackage) {
        this.nameWithPackage = nameWithPackage;
    }

    /**
     * Build {@link JavaInterfaceByteCodeGenerator} object
     * 
     * @return instance of {@link JavaInterfaceByteCodeGenerator}
     */
    public JavaInterfaceByteCodeGenerator buildJava() {
        return new JavaInterfaceByteCodeGenerator(nameWithPackage, methods);
    }

    public GroovyInterfaceScriptGenerator buildGroovy() {
        return new GroovyInterfaceScriptGenerator(nameWithPackage, methods);
    }

    /**
     * Add new interface method
     *
     * @param method method description
     * @return {@code this}
     */
    public InterfaceByteCodeBuilder addAbstractMethod(MethodDescription method) {
        methods.add(Objects.requireNonNull(method, "Method description is null"));
        return this;
    }

    public String getNameWithPackage() {
        return nameWithPackage;
    }

    /**
     * Check if methods were added
     *
     * @return {@code true} if methods are defined
     */
    public boolean isEmpty() {
        return methods.isEmpty();
    }

    /**
     * Create Java Interface Builder with custom class name. Package name will be added by default
     *
     * @param interfaceName java interface name without package
     * @return Java Interface Builder
     */
    public static InterfaceByteCodeBuilder createWithDefaultPackage(String interfaceName) {
        interfaceName = requireNonBlank(interfaceName, "Interface name is null or blank.");
        return new InterfaceByteCodeBuilder(JavaInterfaceByteCodeGenerator.DEFAULT_PACKAGE + interfaceName);
    }

    public static InterfaceByteCodeBuilder create(String packagePath, String interfaceName) {
        interfaceName = requireNonBlank(interfaceName, "Interface name is null or blank.");
        return new InterfaceByteCodeBuilder(packagePath + "." + interfaceName);
    }

    static String requireNonBlank(String str, String message) {
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }
}
