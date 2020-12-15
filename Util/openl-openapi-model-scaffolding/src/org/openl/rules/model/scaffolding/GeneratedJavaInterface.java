package org.openl.rules.model.scaffolding;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

public class GeneratedJavaInterface {

    public static final GeneratedJavaInterface EMPTY = new GeneratedJavaInterface();

    private final String javaNameWithPackage;
    private final String path;
    private final byte[] byteCode;

    private GeneratedJavaInterface() {
        this.path = null;
        this.javaNameWithPackage = null;
        this.byteCode = null;
    }

    public GeneratedJavaInterface(String javaNameWithPackage, byte[] byteCode) {
        this.javaNameWithPackage = Objects.requireNonNull(javaNameWithPackage, "Java Interface name is null.");
        this.path = javaNameWithPackage.replace('.', '/') + ".class";
        this.byteCode = byteCode;
    }

    public String getJavaNameWithPackage() {
        return javaNameWithPackage;
    }

    public String getPath() {
        return path;
    }

    public byte[] getByteCode() {
        return byteCode;
    }

    public InputStream toInputStream() {
        return Optional.ofNullable(byteCode).map(ByteArrayInputStream::new).orElse(null);
    }

    public boolean hasInterface() {
        return byteCode != null && byteCode.length > 0;
    }
}
