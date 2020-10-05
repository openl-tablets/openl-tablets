package org.openl.util.formatters;

import java.util.regex.Pattern;

public class FileNameFormatter {

    private static final Pattern BACK_SLASH_PATTERN = Pattern.compile("\\\\+");

    public static String normalizePath(String path) {
        return BACK_SLASH_PATTERN.matcher(path).replaceAll("/");
    }

}
