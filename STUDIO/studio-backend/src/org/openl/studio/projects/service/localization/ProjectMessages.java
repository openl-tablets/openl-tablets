package org.openl.studio.projects.service.localization;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.openl.message.OpenLMessage;

/**
 * The messages of a project as the session read them: a language for each localization file, a key for each message
 * any of them writes, and the messages of the projects it depends on.
 *
 * <p>It is read once and kept, so a page of keys is answered without reading a file. It never changes: a write of the
 * files gives a new one.
 *
 * <p>A language that lacks a key takes it as the engine does: from the same language of a project it depends on,
 * then from its parent language, German of Luxembourg falling back to German and then to the default language.
 */
public final class ProjectMessages {

    /**
     * The messages a project the project depends on gives.
     *
     * @param project the name of the project
     * @param folder  the folder of the project
     * @param texts   the texts of every key by the language
     */
    public record Shared(String project, Path folder, Map<String, Map<String, @Nullable String>> texts) {
    }

    /** A text a language shows for a key, and where it comes from. */
    public sealed interface Shown {

        /** The text of the language itself. */
        record Own(String text) implements Shown {
        }

        /**
         * The text of another language, or of the language of another project.
         *
         * @param from    the language the text comes from
         * @param project the project the text comes from, {@code null} for this one
         */
        record Inherited(String text, String from, @Nullable String project) implements Shown {
        }

        /** No text: the rule gets the key itself. */
        record Missing() implements Shown {
        }
    }

    private final Path folder;
    private final List<MessageFile> files;
    private final List<Shared> shared;
    private final List<String> keys;
    private final Map<String, Map<String, @Nullable String>> texts = new LinkedHashMap<>();
    private final AtomicReference<@Nullable List<OpenLMessage>> problems = new AtomicReference<>();

    /**
     * @param folder the folder of the project
     * @param files  the localization files of the project
     * @param shared the messages of the projects it depends on, the nearest first
     */
    ProjectMessages(Path folder, List<MessageFile> files, List<Shared> shared) {
        this.folder = folder;
        this.files = files.stream().sorted(ProjectMessages::byLanguage).toList();
        this.shared = List.copyOf(shared);
        var all = new LinkedHashSet<String>();
        this.files.forEach(file -> {
            var fileTexts = file.texts();
            texts.put(file.tag(), fileTexts);
            all.addAll(fileTexts.keySet());
        });
        this.keys = List.copyOf(all);
    }

    /** The folder of the project. */
    public Path folder() {
        return folder;
    }

    /** The files of the languages, the default one first, then the others by their tags. */
    public List<MessageFile> files() {
        return files;
    }

    /** The file of a language. */
    public Optional<MessageFile> fileOf(String tag) {
        return files.stream().filter(file -> file.tag().equals(tag)).findFirst();
    }

    /** The keys in the order they are written: those of the default file first, then the rest. */
    public List<String> keys() {
        return keys;
    }

    /** Whether a language of the project writes the key. */
    public boolean has(String key) {
        return texts.values().stream().anyMatch(languageTexts -> languageTexts.containsKey(key));
    }

    /** The texts of a key by the language, {@code null} for a key written without a text. */
    public Map<String, @Nullable String> textsOf(String key) {
        var found = new LinkedHashMap<String, @Nullable String>();
        texts.forEach((tag, languageTexts) -> {
            if (languageTexts.containsKey(key)) {
                found.put(tag, languageTexts.get(key));
            }
        });
        return found;
    }

    /** The folder of a project the project depends on, by its name. */
    public Optional<Path> folderOf(String project) {
        return shared.stream()
                .filter(dependency -> dependency.project().equals(project))
                .map(Shared::folder)
                .findFirst();
    }

    /**
     * The folder of the project a key leads to: the nearest project the project depends on that gives the key a default
     * text. Empty while the project writes the key in any language, since its own messages are where it is edited.
     */
    public Optional<Path> ownerOf(String key) {
        if (has(key)) {
            return Optional.empty();
        }
        return shared.stream()
                .filter(dependency -> dependency.texts().getOrDefault("", Map.of()).get(key) != null)
                .map(Shared::folder)
                .findFirst();
    }

    /** The text the nearest project the project depends on gives a key in a language. */
    public Optional<Shown.Inherited> sharedTextOf(String tag, String key) {
        for (var dependency : shared) {
            var text = dependency.texts().getOrDefault(tag, Map.of()).get(key);
            if (text != null) {
                return Optional.of(new Shown.Inherited(text, tag, dependency.project()));
            }
        }
        return Optional.empty();
    }

    /**
     * What a language shows for a key: its own text, or else the text of the nearest language it falls back to, each
     * of the project first and then of the projects it depends on, or nothing when none has it.
     *
     * <p>A key written without a text stops the search: the rule gets the key itself.
     */
    public Shown shownIn(String tag, String key) {
        var own = texts.getOrDefault(tag, Map.of());
        if (own.get(key) instanceof String text) {
            return new Shown.Own(text);
        }
        for (var language : withParents(tag)) {
            var languageTexts = texts.getOrDefault(language, Map.of());
            if (languageTexts.get(key) instanceof String text) {
                return new Shown.Inherited(text, language, null);
            }
            var fromShared = sharedTextOf(language, key);
            if (fromShared.isPresent()) {
                return fromShared.get();
            }
            if (languageTexts.containsKey(key)) {
                return new Shown.Missing();
            }
        }
        return new Shown.Missing();
    }

    /**
     * Whether a language shows the default text of the project for a key, having no text of its own, of a parent
     * language or of a project it depends on: a translation is wanted.
     */
    public boolean showsDefaultText(String tag, String key) {
        return !tag.isEmpty()
                && shownIn(tag, key) instanceof Shown.Inherited inherited
                && inherited.from().isEmpty()
                && inherited.project() == null;
    }

    /**
     * What the check of the files found, checked the first time it is asked for and kept.
     *
     * @param check checks the files
     */
    public List<OpenLMessage> problems(Supplier<List<OpenLMessage>> check) {
        var found = problems.get();
        if (found == null) {
            found = List.copyOf(check.get());
            problems.compareAndSet(null, found);
        }
        return found;
    }

    /** A language and the languages it falls back to, nearest first: {@code de_LU} → {@code de_LU, de, ""}. */
    static List<String> withParents(String tag) {
        var languages = new ArrayList<String>();
        languages.add(tag);
        var parent = tag;
        while (!parent.isEmpty()) {
            var cut = parent.lastIndexOf('_');
            parent = cut < 0 ? "" : parent.substring(0, cut);
            languages.add(parent);
        }
        return languages;
    }

    private static int byLanguage(MessageFile one, MessageFile other) {
        return one.tag().compareTo(other.tag());
    }
}
