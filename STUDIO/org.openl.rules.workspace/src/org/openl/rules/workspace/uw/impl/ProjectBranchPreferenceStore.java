package org.openl.rules.workspace.uw.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;

import org.openl.util.PropertiesUtils;

/**
 * Persists the selected branch of closed projects in one user workspace.
 *
 * <p>The store is user-scoped and contains preferences only. Project membership remains derived from repository trees.
 * Invalid preferences are removed by the workspace after it checks the current readable branch set.
 */
@NullMarked
@Slf4j
public final class ProjectBranchPreferenceStore {

    static final String FILE_NAME = ".project-branches.properties";
    private static final String TEMP_SUFFIX = ".tmp";

    private final Path file;
    private final Map<String, String> preferences = new HashMap<>();

    private ProjectBranchPreferenceStore(Path userDirectory) {
        file = userDirectory.resolve(FILE_NAME);
        load();
    }

    public static ProjectBranchPreferenceStore open(Path userDirectory) {
        return new ProjectBranchPreferenceStore(userDirectory);
    }

    public synchronized Optional<String> get(String repositoryId, String projectName) {
        return Optional.ofNullable(preferences.get(key(repositoryId, projectName)));
    }

    public synchronized void put(String repositoryId, String projectName, String branch) {
        if (!branch.equals(preferences.put(key(repositoryId, projectName), branch))) {
            save();
        }
    }

    public synchronized void remove(String repositoryId, String projectName) {
        if (preferences.remove(key(repositoryId, projectName)) != null) {
            save();
        }
    }

    private void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            PropertiesUtils.load(file, preferences::put);
        } catch (IOException | IllegalArgumentException e) {
            preferences.clear();
            log.warn("Cannot read project branch preferences from '{}'.", file, e);
        }
    }

    private void save() {
        var temporary = file.resolveSibling(file.getFileName() + TEMP_SUFFIX);
        try {
            Files.createDirectories(file.getParent());
            PropertiesUtils.store(temporary, new TreeMap<>(preferences).entrySet());
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException | FileAlreadyExistsException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.warn("Cannot save project branch preferences to '{}'.", file, e);
        }
    }

    private static String key(String repositoryId, String projectName) {
        var identity = repositoryId + '\0' + projectName.toLowerCase(Locale.ROOT);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(identity.getBytes(StandardCharsets.UTF_8));
    }
}
