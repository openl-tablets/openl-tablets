package org.openl.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openl.util.StringUtils;

/**
 * Java Interface builder
 *
 * @author Vladyslav Pikus
 */
public class JavaInterfaceByteCodeBuilder {

    private final String nameWithPackage;
    private final List<MethodDescription> methods = new ArrayList<>();

    private JavaInterfaceByteCodeBuilder(String nameWithPackage) {
        this.nameWithPackage = nameWithPackage;
    }

    /**
     * Build {@link JavaInterfaceByteCodeGenerator} object
     * @return instance of {@link JavaInterfaceByteCodeGenerator}
     */
    public JavaInterfaceByteCodeGenerator build() {
        return new JavaInterfaceByteCodeGenerator(nameWithPackage, methods);
    }

    /**
     * Add new interface method
     *
     * @param method method description
     * @return {@code this}
     */
    public JavaInterfaceByteCodeBuilder addAbstractMethod(MethodDescription method) {
        methods.add(Objects.requireNonNull(method, "Method description is null"));
        return this;
    }

    public String getNameWithPackage() {
        return nameWithPackage;
    }

    /**
     * Create Java Interface Builder with custom class name. Package name will be added by default
     *
     * @param interfaceName java interface name without package
     * @return Java Interface Builder
     */
    public static JavaInterfaceByteCodeBuilder createWithDefaultPackage(String interfaceName) {
        interfaceName = requireNonBlank(interfaceName, "Interface name is null or blank.");
        return new JavaInterfaceByteCodeBuilder(JavaInterfaceByteCodeGenerator.DEFAULT_PACKAGE + interfaceName);
    }

    static String requireNonBlank(String str, String message) {
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }
}
