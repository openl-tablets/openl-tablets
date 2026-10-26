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
    private static final String IN_PLACE_HISTORY = ".in-place";

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
        return normalizePath(resolveHistoryFolder(project).resolve(module.getRulesRootPath()));
    }

    /**
     * Returns the edit-history folder of the project relative to the user workspace directory:
     * {@code .history/<project folder>}.
     *
     * <p>A project edited in place keeps its history in {@code .history/.in-place/<repository id>/<project path>}.
     * Its path is unique only within its repository, and it may equal the folder of a copy in the workspace, whose
     * name never starts with a dot.
     */
    public static Path resolveHistoryFolder(RulesProject project) {
        var history = Path.of(HISTORY_FOLDER);
        return project.isInPlace()
                ? history.resolve(IN_PLACE_HISTORY).resolve(project.getLocalRepository().getId())
                        .resolve(project.getFolderPath())
                : history.resolve(project.getFolderPath());
    }

    private static String normalizePath(Path p) {
        return p.toString().replace('\\', '/');
    }
}
