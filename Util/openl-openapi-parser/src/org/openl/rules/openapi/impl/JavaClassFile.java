package org.openl.rules.openapi.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

public class JavaClassFile {

    private final String javaNameWithPackage;
    private final String path;
    private final byte[] byteCode;

    public JavaClassFile(String javaNameWithPackage, byte[] byteCode) {
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

    public boolean isEmpty() {
        return byteCode != null && byteCode.length > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JavaClassFile that = (JavaClassFile) o;
        return Objects.equals(javaNameWithPackage, that.javaNameWithPackage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(javaNameWithPackage);
    }

    @Override
    public String toString() {
        return javaNameWithPackage;
    }
}
