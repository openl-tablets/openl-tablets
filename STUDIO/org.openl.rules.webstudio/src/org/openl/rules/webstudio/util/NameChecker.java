package org.openl.rules.webstudio.util;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;

/*
 * Checks whether specified string can be used to name project artefact.
 *
 * @author Aleh Bykhavets
 */
public final class NameChecker {
    private static final char[] FORBIDDEN_CHARS = { '\\', '/', ':', ';', '<', '>', '?', '*', '%', '\'', '[', ']' };
    private static String forbiddenChars;
    public static final String BAD_NAME_MSG = "Name can not contain forbidden characters (" + NameChecker
        .getForbiddenCharacters() + "), start with space, end with space or dot!";
    public static final String FOLDER_EXISTS = "Cannot create folder because folder with such name already exists.";
    public static final String FOLDER_NAME_EMPTY = "Folder name must not be empty.";
    public static final String BAD_PROJECT_NAME_MSG = "Project name can not contain forbidden characters (" + NameChecker
        .getForbiddenCharacters() + "), special characters, start with space, end with space or dot!";

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

        // JCR path cannot starts with space
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
        if (forbiddenChars == null) {
            // generate string: "\, /, :, ;, <, >, ?, *, %, ', [, ]"
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < FORBIDDEN_CHARS.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }

                sb.append(FORBIDDEN_CHARS[i]);
            }
            forbiddenChars = sb.toString();
        }

        return forbiddenChars;
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
            // Such folder isn't present
            return false;
        }
    }
}
