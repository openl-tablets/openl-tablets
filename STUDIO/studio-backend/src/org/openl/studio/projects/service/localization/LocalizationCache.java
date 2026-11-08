package org.openl.studio.projects.service.localization;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import lombok.Synchronized;
import org.jspecify.annotations.Nullable;

import org.openl.rules.binding.OpenLMessageSource;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.validation.LocalizationCheck;

/**
 * The messages of the project the session reads the localization of, kept between its requests.
 *
 * <p>A file is read once and kept until it changes: each request looks at the size and the time of the last change of
 * the files, so a file written by anything — the files of the project, a sync, a revision opened — is read again, and
 * the others are not. A write the session makes tells which files it wrote, so they are read again even when the clock
 * of the file system cannot tell.
 *
 * <p>It keeps the messages of one project at a time: asking for another one forgets the first.
 */
public final class LocalizationCache {

    private @Nullable Path folder;
    private final Map<Path, MessageFile> read = new HashMap<>();
    private @Nullable ProjectMessages last;

    /**
     * The messages of a project, read again where a file changed since they were read.
     *
     * @param project      the project
     * @param dependencies the projects it depends on, directly or through others, the nearest first
     * @return the messages
     */
    @Synchronized
    public ProjectMessages messagesOf(ProjectDescriptor project, List<ProjectDescriptor> dependencies) {
        var projectFolder = project.getProjectFolder();
        if (!projectFolder.equals(folder)) {
            forgetAll();
            folder = projectFolder;
        }
        var wanted = new HashMap<Path, MessageFile>();
        var files = readFiles(projectFolder, wanted);
        var dependencyFiles = dependencies.stream()
                .map(dependency -> Map.entry(dependency, readFiles(dependency.getProjectFolder(), wanted)))
                .toList();
        var kept = last;
        var unchanged = wanted.keySet().equals(read.keySet())
                && wanted.entrySet().stream().allMatch(entry -> read.get(entry.getKey()) == entry.getValue());
        read.clear();
        read.putAll(wanted);
        if (kept != null && unchanged) {
            return kept;
        }
        var shared = dependencyFiles.stream()
                .map(dependency -> new ProjectMessages.Shared(dependency.getKey().getName(),
                        dependency.getKey().getProjectFolder(),
                        textsOf(dependency.getValue())))
                .toList();
        var messages = new ProjectMessages(projectFolder, files, shared);
        last = messages;
        return messages;
    }

    /** Forgets the files the session wrote, a file or every file of a folder, so they are read again. */
    @Synchronized
    public void forget(Path written) {
        read.keySet().removeIf(path -> path.startsWith(written));
        last = null;
    }

    /** Forgets everything, when the model lets go of the project. */
    @Synchronized
    public void clear() {
        forgetAll();
        folder = null;
    }

    private void forgetAll() {
        read.clear();
        last = null;
    }

    /** The localization files of a project folder, each read again unless it is kept and did not change. */
    private List<MessageFile> readFiles(Path projectFolder, Map<Path, MessageFile> wanted) {
        var files = new ArrayList<MessageFile>();
        var i18n = projectFolder.resolve(OpenLMessageSource.FOLDER);
        if (!Files.isDirectory(i18n)) {
            return files;
        }
        try (Stream<Path> listed = Files.list(i18n)) {
            listed.forEach(path -> {
                var matcher = LocalizationCheck.FILE.matcher(path.getFileName().toString());
                if (matcher.matches() && Files.isRegularFile(path)) {
                    var tag = Objects.requireNonNullElse(matcher.group(1), "");
                    var kept = read.get(path);
                    var file = kept != null && kept.stamp().equals(MessageFile.Stamp.of(path))
                            ? kept
                            : MessageFile.read(path, tag);
                    wanted.put(path, file);
                    files.add(file);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }

    private static Map<String, Map<String, @Nullable String>> textsOf(List<MessageFile> files) {
        var texts = new LinkedHashMap<String, Map<String, @Nullable String>>();
        files.forEach(file -> texts.put(file.tag(), file.texts()));
        return texts;
    }
}
