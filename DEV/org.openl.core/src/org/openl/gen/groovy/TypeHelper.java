package org.openl.gen.groovy;

import java.util.Set;
import java.util.regex.Pattern;

public class TypeHelper {

    private static final Pattern ARRAY_MATCHER = Pattern.compile("[\\[\\]]");

    public static final Set<String> DEFAULT_IMPORTS = Set.of("java.lang.Object",
            "java.lang.Character",
            "java.lang.Class",
            "java.lang.Enum",
            "java.lang.Float",
            "java.lang.Long",
            "java.lang.Short",
            "java.lang.Double",
            "java.lang.CharSequence",
            "java.lang.Integer",
            "java.lang.Byte",
            "java.lang.Void");

    public static final Set<String> PRIMITIVES = Set
            .of("byte", "short", "int", "long", "float", "double", "char", "boolean", "void");

    protected TypeHelper() {
    }

    public static String removeArrayBrackets(String name) {
        return ARRAY_MATCHER.matcher(name).replaceAll("");
    }

    public static String makeImported(String nameWithPackage) {
        return nameWithPackage.substring(nameWithPackage.lastIndexOf(".") + 1);
    }
}
