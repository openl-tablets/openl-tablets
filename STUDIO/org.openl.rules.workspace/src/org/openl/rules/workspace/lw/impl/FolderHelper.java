package org.openl.rules.workspace.lw.impl;

import java.nio.file.Path;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;

/**
 * Folder (File System) Helper for Local Workspace.
 *
 * @author Aleh Bykhavets
 */
public final class FolderHelper {

    public static final String HISTORY_FOLDER = ".history";

    private FolderHelper() {
    }

    /**
     * Checks that the name is usable as a top-level folder name in a user workspace.
     *
     * <p>A blank name or a path separator would escape or split the workspace folder. A leading dot
     * would turn the folder into a hidden service folder, like {@code .history} or {@code .metainfo}.
     */
    public static boolean isSafeFolderName(String name) {
        return !name.isBlank()
                && !name.startsWith(".")
                && !name.contains("/")
                && !name.contains("\\");
    }

    /**
     * Returns the module edit-history folder relative to the user workspace directory.
     *
     * <p>The history lives outside the project folder: {@code .history/<project>/<module root>}.
     */
    public static String resolveHistoryFolder(RulesProject project, Module module) {
        return normalizePath(Path.of(HISTORY_FOLDER, project.getFolderPath(), module.getRulesRootPath()));
    }

    /**
     * Returns the module edit-history folder for the project located at the given folder.
     *
     * <p>The history is a sibling of the project folder: {@code <user dir>/.history/<project>/<module root>}.
     */
    public static Path resolveHistoryFolder(Path projectFolder, Module module) {
        return projectFolder.resolveSibling(HISTORY_FOLDER)
                .resolve(projectFolder.getFileName().toString())
                .resolve(module.getRulesRootPath());
    }

    private static String normalizePath(Path p) {
        return p.toString().replace('\\', '/');
    }
}
