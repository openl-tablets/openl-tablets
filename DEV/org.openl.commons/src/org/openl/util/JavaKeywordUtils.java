package org.openl.util;

import java.util.HashSet;
import java.util.Set;

public class JavaKeywordUtils {

    private static final Set<String> JAVA_KEYWORDS = new HashSet<>();

    static {
        JAVA_KEYWORDS.add("abstract");
        JAVA_KEYWORDS.add("continue");
        JAVA_KEYWORDS.add("for");
        JAVA_KEYWORDS.add("new");
        JAVA_KEYWORDS.add("switch");
        JAVA_KEYWORDS.add("assert");
        JAVA_KEYWORDS.add("default");
        JAVA_KEYWORDS.add("goto");
        JAVA_KEYWORDS.add("package");
        JAVA_KEYWORDS.add("synchronized");
        JAVA_KEYWORDS.add("boolean");
        JAVA_KEYWORDS.add("do");
        JAVA_KEYWORDS.add("if");
        JAVA_KEYWORDS.add("private");
        JAVA_KEYWORDS.add("this");
        JAVA_KEYWORDS.add("break");
        JAVA_KEYWORDS.add("double");
        JAVA_KEYWORDS.add("implements");
        JAVA_KEYWORDS.add("protected");
        JAVA_KEYWORDS.add("throw");
        JAVA_KEYWORDS.add("byte");
        JAVA_KEYWORDS.add("else");
        JAVA_KEYWORDS.add("import");
        JAVA_KEYWORDS.add("public");
        JAVA_KEYWORDS.add("throws");
        JAVA_KEYWORDS.add("case");
        JAVA_KEYWORDS.add("enum");
        JAVA_KEYWORDS.add("instanceof");
        JAVA_KEYWORDS.add("return");
        JAVA_KEYWORDS.add("transient");
        JAVA_KEYWORDS.add("catch");
        JAVA_KEYWORDS.add("extends");
        JAVA_KEYWORDS.add("int");
        JAVA_KEYWORDS.add("short");
        JAVA_KEYWORDS.add("try");
        JAVA_KEYWORDS.add("char");
        JAVA_KEYWORDS.add("final");
        JAVA_KEYWORDS.add("interface");
        JAVA_KEYWORDS.add("static");
        JAVA_KEYWORDS.add("void");
        JAVA_KEYWORDS.add("class");
        JAVA_KEYWORDS.add("finally");
        JAVA_KEYWORDS.add("long");
        JAVA_KEYWORDS.add("strictfp");
        JAVA_KEYWORDS.add("volatile");
        JAVA_KEYWORDS.add("const");
        JAVA_KEYWORDS.add("float");
        JAVA_KEYWORDS.add("native");
        JAVA_KEYWORDS.add("while");
    }

    public static boolean isJavaKeyword(String s) {
        return JAVA_KEYWORDS.contains(s);
    }

    public static String toJavaIdentifier(String s) {
        if (s == null) {
            return null;
        }
        if (!s.isEmpty()) {
            s = s.replaceAll("\\s+", "_"); // Replace whitespaces
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                if (Character.isJavaIdentifierPart(s.charAt(i))) {
                    sb.append(s.charAt(i));
                }
            }
            s = sb.toString();
            if (isJavaKeyword(s) || !s.isEmpty() && !Character.isJavaIdentifierStart(s.charAt(0))) {
                s = "_" + s;
            }
        }
        return s;
    }
}
