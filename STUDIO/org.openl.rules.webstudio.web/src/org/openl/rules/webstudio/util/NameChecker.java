package org.openl.rules.webstudio.util;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;

/*
 * Checks whether specified string can be used to name project artefact.
 *
 * @author Aleh Bykhavets
 */
public final class NameChecker {
    private static final char[] FORBIDDEN_CHARS = {'\\', '/', ':', ';', '<', '>', '?', '*', '%', '\'', '[', ']', '|', '"'};
    public static final String FORBIDDEN_CHARS_STRING = "\\, /, :, ;, <, >, ?, *, %, ', [, ], |, \"";
    public static final String BAD_NAME_MSG = "Name cannot contain forbidden characters (" + FORBIDDEN_CHARS_STRING + "), start with space, end with space or dot.";
    public static final String FOLDER_EXISTS = "Cannot create folder because folder with such name already exists.";
    public static final String FOLDER_NAME_EMPTY = "Folder name must not be empty.";
    public static final String BAD_PROJECT_NAME_MSG = "Project name cannot contain forbidden characters (" + FORBIDDEN_CHARS_STRING + "), special characters, start with space, end with space or dot.";
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

    protected static boolean checkForbiddenChars(String artefactName) {
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
        if (artefactName.isEmpty()) {
            return false;
        }

        // seems OK
        return true;
    }

    /**
     * Checks if the path is valid (cross-platform).
     *
     * @param path path where folder
     * @throws IOException If path is invalid for any platform
     */
    public static void validatePath(String path) throws IOException {
        var end = path.length() - 1;
        for (int i=0; i <= end; i++) {
            switch (path.charAt(i)) {
                case '/', '\\':
                    if (i < end && (path.charAt(i + 1) == '/' || path.charAt(i + 1) == '\\' )) {
                        throw new IOException(BAD_NAME_MSG);
                    }
                    break;
                case ';', '<', '>', '?', '*', '%', '\'', '"', '|', '[', ']':
                    throw new IOException(BAD_NAME_MSG);
                default:
                    break;
            }
        }
        for (var p : Paths.get(path)) {
            var name = p.toString();
            if (!checkName(name)) {
                throw new IOException(BAD_NAME_MSG);
            }

            if (isReservedName(name)) {
                throw new IOException(String.format("'%s' is a reserved word.", name));
            }
        }
    }

    public static boolean isReservedName(String name) {
        return RESERVED_WORDS.contains(name);
    }

    protected static boolean checkSpecialChars(String artefactName) {
        // check for special chars
        for (int i = 0; i < artefactName.length(); i++) {
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

    public static boolean checkIsFolderPresent(AProjectFolder folder, String folderName) {
        try {
            AProjectArtefact artefact = folder.getArtefact(folderName);

            return artefact instanceof AProjectFolder;

        } catch (ProjectException e1) {
            String parentPath = folder.getInternalPath();
            String prefix = parentPath.isEmpty() ? "" : parentPath + "/";
            prefix += folderName + "/";

            for (AProjectArtefact artefact : folder.getArtefacts()) {
                if (artefact.getInternalPath().startsWith(prefix)) {
                    return true;
                }
            }
            // Such folder is not present
            return false;
        }
    }
}
