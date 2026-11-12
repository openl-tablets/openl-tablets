package org.openl.rules.webstudio.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/**
 * Checks whether specified string can be used to name project artefact.
 *
 * @author Aleh Bykhavets
 */
public final class NameChecker {
    private static final char[] FORBIDDEN_CHARS = {'\\', '/', ':', ';', '<', '>', '?', '*', '%', '\'', '[', ']', '|', '"'};
    public static final String FORBIDDEN_CHARS_STRING = "\\, /, :, ;, <, >, ?, *, %, ', [, ], |, \"";
    public static final String BAD_NAME_MSG = "Name cannot contain forbidden characters (" + FORBIDDEN_CHARS_STRING + "), start with space, end with space or dot.";
    private static final Set<String> RESERVED_WORDS = Stream
            .of("CON",
                    "PRN",
                    "AUX",
                    "NUL",
                    "COM1",
                    "COM2",
                    "COM3",
                    "COM4",
                    "COM5",
                    "COM6",
                    "COM7",
                    "COM8",
                    "COM9",
                    "LPT1",
                    "LPT2",
                    "LPT3",
                    "LPT4",
                    "LPT5",
                    "LPT6",
                    "LPT7",
                    "LPT8",
                    "LPT9")
            .collect(Collectors.toSet());

    private NameChecker() {
    }

    static boolean checkForbiddenChars(String artefactName) {
        // check for forbidden chars
        for (char c : FORBIDDEN_CHARS) {
            if (artefactName.indexOf(c) >= 0) {
                // contains forbidden (bad) characters
                return false;
            }
        }

        // no forbidden chars
        return true;
    }

    public static boolean checkName(String artefactName) {
        if (!checkForbiddenChars(artefactName)) {
            return false;
        }

        if (!checkSpecialChars(artefactName)) {
            return false;
        }

        // Path cannot start with space
        if (artefactName.startsWith(" ")) {
            return false;
        }
        // Windows File System issues
        if (artefactName.endsWith(" ")) {
            return false;
        }
        if (artefactName.endsWith(".")) {
            return false;
        }

        // check empty name
        return !artefactName.isEmpty();
    }

    /**
     * Checks if the path is valid (cross-platform) for a new file or folder.
     *
     * @param path path where folder
     * @throws IOException If path is invalid for any platform
     */
    public static void validatePath(String path) throws IOException {
        validateExistingPath(path);
        var reserved = findReservedName(path);
        if (reserved != null) {
            throw new IOException("'%s' is a reserved word.".formatted(reserved));
        }
    }

    /**
     * Checks the path of a file or folder that already exists.
     *
     * <p>A name reserved by Windows is accepted here: a repository filled on another platform may hold such a file,
     * and it must stay readable, movable and removable.
     *
     * @param path path of the file or folder
     * @throws IOException If the path holds a forbidden character or name
     */
    // A when guard is allowed only on a pattern label, not on the constant labels this switch dispatches on.
    @SuppressWarnings("java:S6916")
    public static void validateExistingPath(String path) throws IOException {
        var end = path.length() - 1;
        for (var i=0; i <= end; i++) {
            switch (path.charAt(i)) {
                case '/', '\\' -> {
                    if (i < end && (path.charAt(i + 1) == '/' || path.charAt(i + 1) == '\\')) {
                        throw new IOException(BAD_NAME_MSG);
                    }
                }
                case ';', '<', '>', '?', '*', '%', '\'', '"', '|', '[', ']' -> throw new IOException(BAD_NAME_MSG);
                default -> { /* OK */ }
            }
        }
        for (var p : Path.of(path)) {
            var name = p.toString();
            if (!checkName(name)) {
                throw new IOException(BAD_NAME_MSG);
            }
        }
    }

    /**
     * Tells whether Windows reserves the name for a device.
     *
     * <p>Windows reserves such a name in any letter case and with any extension: {@code con}, {@code Con.xlsx}.
     */
    public static boolean isReservedName(String name) {
        var extension = name.indexOf('.');
        return RESERVED_WORDS.contains((extension < 0 ? name : name.substring(0, extension)).toUpperCase(Locale.ROOT));
    }

    /**
     * Finds the name in a path that Windows reserves for a device.
     *
     * @return the first reserved name of the path, or {@code null} when it has none
     */
    public static @Nullable String findReservedName(String path) {
        return Arrays.stream(path.split("[/\\\\]")).filter(NameChecker::isReservedName).findFirst().orElse(null);
    }

    static boolean checkSpecialChars(String artefactName) {
        // check for special chars
        for (var i = 0; i < artefactName.length(); i++) {
            if (artefactName.charAt(i) < 32) {
                // contains (bad) special characters (\t, \n, all that less than <space>)
                return false;
            }
        }

        // No special chars
        return true;
    }

    public static String getForbiddenCharacters() {
        return FORBIDDEN_CHARS_STRING;
    }

}
