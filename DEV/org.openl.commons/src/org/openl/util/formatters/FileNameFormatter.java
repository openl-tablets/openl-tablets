package org.openl.util.formatters;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class FileNameFormatter {

    private static final Pattern BACK_SLASH_PATTERN = Pattern.compile("\\\\+");
    private static final String NORMALIZED_SEPARATOR = "/";
    private static final String DEFAULT_SEPARATOR = FileSystems.getDefault().getSeparator();

    private FileNameFormatter() {
    }

    public static String normalizePath(String path) {
        return BACK_SLASH_PATTERN.matcher(path).replaceAll(NORMALIZED_SEPARATOR);
    }

    public static String normalizePath(Path path) {
        return normalizePath(path.toString());
    }

    public static Path fromNormalizedPath(String path) {
        return Paths.get(path.replace(NORMALIZED_SEPARATOR, DEFAULT_SEPARATOR));
    }
}
