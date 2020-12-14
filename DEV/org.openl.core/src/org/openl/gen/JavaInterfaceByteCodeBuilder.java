package org.openl.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openl.util.StringUtils;

/**
 * @author Vladyslav Pikus
 */
public class JavaInterfaceByteCodeBuilder {

    private final String nameWithPackage;
    private final List<MethodDescription> methods = new ArrayList<>();

    private JavaInterfaceByteCodeBuilder(String nameWithPackage) {
        this.nameWithPackage = nameWithPackage;
    }

    public byte[] buildAndGetByteCode() {
        return new JavaInterfaceByteCodeGenerator(nameWithPackage, methods).byteCode();
    }

    public JavaInterfaceByteCodeBuilder addAbstractMethod(MethodDescription method) {
        methods.add(Objects.requireNonNull(method, "Method description is null"));
        return this;
    }

    public static JavaInterfaceByteCodeBuilder createWithDefaultPackage(String nameWithPackage) {
        nameWithPackage = requireNonBlank(nameWithPackage, "Interface name is null or blank.");
        return new JavaInterfaceByteCodeBuilder(JavaInterfaceByteCodeGenerator.DEFAULT_PACKAGE + nameWithPackage);
    }

    static String requireNonBlank(String str, String message) {
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }
}
