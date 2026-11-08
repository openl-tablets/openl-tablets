package org.openl.rules.validation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.message.OpenLFileMessage;
import org.openl.message.OpenLMessage;
import org.openl.rules.binding.OpenLMessageSource;
import org.openl.source.SourceRange;
import org.openl.util.PropertiesUtils;

/**
 * Checks the localization files of a project: the {@code message*.properties} files of its {@code i18n} folder.
 *
 * <p>Every problem is a warning, as a rule asking for a missing or broken message still runs. A text is reported
 * when its key is written twice in a file, when its pattern is broken, when its placeholders differ from those of
 * the default language, and when the default language has no text for its key. A broken pattern is told piece by
 * piece, where each problem is in the text; the same problem with the same piece, such as two full-width commas, is
 * told once, with every place it is in. A language that leaves keys of the
 * default language untranslated gets one warning with their count; a regional language that inherits the text of
 * its parent language translates it.
 *
 * <p>A key is read as the engine reads it: anything before the first equals sign or colon, spaces inside it
 * included, such as {@code My some key = Some text}. A line without either has a key and no text, not an empty
 * one: a rule asking for it gets the key itself, which is reported too.
 *
 * <p>Only the files of the project itself are checked, and only the files: nothing of the project has to be compiled,
 * so OpenL Studio checks them before its modules, and again whenever they are written. A key the default files of a
 * dependency or a library hold is not missing, and its translations take their placeholders from that text.
 */
@Slf4j
public final class LocalizationCheck {

    /**
     * The tag of a language other than the default one, as a file name writes it: a language of two or three letters,
     * then the country and the variant, each after an underscore, as {@code de}, {@code de_LU} or {@code de__POSIX}.
     */
    public static final String LANGUAGE_TAG = "[A-Za-z]{2,3}(?:_\\w*[A-Za-z0-9])?";
    /**
     * The locale a language tag names: its language, country and variant, joined by underscores. The engine names the
     * file of a locale by it, the language in lower case and the country in upper case.
     *
     * @param tag the tag, empty for the default language
     * @return the locale, {@link Locale#ROOT} for the default language
     */
    public static Locale localeOf(String tag) {
        if (tag.isEmpty()) {
            return Locale.ROOT;
        }
        var parts = Arrays.copyOf(tag.split("_", 3), 3);
        return Locale.of(parts[0], Objects.requireNonNullElse(parts[1], ""), Objects.requireNonNullElse(parts[2], ""));
    }

    /** The name of a localization file: {@code message.properties}, or {@code message_} with a language tag. */
    public static final Pattern FILE = Pattern.compile("message(?:_(" + LANGUAGE_TAG + "))?\\.properties");

    /**
     * One localization file of the project: its language and what it writes, line by line. A key written on a
     * line without a separator has a null text: a rule asking for it gets the key itself.
     */
    private record Language(String tag,
                            Path file,
                            List<PropertiesUtils.Entry> entries,
                            Map<String, @Nullable String> texts) {

        String name() {
            return tag.isEmpty() ? "the default language" : locale().getDisplayName(Locale.ENGLISH);
        }

        String fileName() {
            return file.getFileName().toString();
        }

        /** The locale the engine reads the language in: its language, country and variant. */
        Locale locale() {
            return localeOf(tag);
        }

        /** The tags a key missing here is looked for in, nearest first, the default one left out. */
        List<String> parents() {
            var parts = tag.split("_");
            var parents = new ArrayList<String>();
            for (var length = parts.length - 1; length > 0; length--) {
                parents.add(String.join("_", List.of(parts).subList(0, length)));
            }
            return parents;
        }
    }

    /**
     * What the check of a project found.
     *
     * @param files    whether the project has a localization file at all
     * @param messages the warnings about its files, none for a project without them
     */
    public record Result(boolean files, List<OpenLMessage> messages) {
    }

    private LocalizationCheck() {
    }

    /**
     * Checks the localization files of a project.
     *
     * @param projectFolder the folder of the project
     * @param source        the messages a rule of the project reads: those of the project, its dependencies and its
     *                      libraries
     * @return whether the project has localization files, and the warnings about them
     */
    public static Result check(Path projectFolder, OpenLMessageSource source) {
        var messages = new ArrayList<OpenLMessage>();
        var languages = read(projectFolder.resolve(OpenLMessageSource.FOLDER), messages);
        var defaultLanguage = languages.get("");
        languages.values().forEach(language -> check(language, defaultLanguage, source, messages));
        if (defaultLanguage != null) {
            languages.values().stream()
                    .filter(language -> !language.tag().isEmpty())
                    .forEach(language -> checkTranslated(language, defaultLanguage, languages, messages));
        }
        return new Result(!languages.isEmpty(), List.copyOf(messages));
    }

    /** The localization files of the folder by their tags, the default one first; a file not read is told. */
    private static Map<String, Language> read(Path folder, List<OpenLMessage> messages) {
        var languages = new LinkedHashMap<String, Language>();
        if (!Files.isDirectory(folder)) {
            return languages;
        }
        try (Stream<Path> files = Files.list(folder)) {
            files.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::getFileName))
                    .forEach(file -> readLanguage(file, messages)
                            .ifPresent(language -> languages.put(language.tag(), language)));
        } catch (IOException e) {
            log.warn("Failed to list the localization files of '{}'", folder, e);
        }
        return languages;
    }

    private static Optional<Language> readLanguage(Path file, List<OpenLMessage> messages) {
        var matcher = FILE.matcher(file.getFileName().toString());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        var tag = matcher.group(1) == null ? "" : matcher.group(1);
        // Read as the engine reads it: in UTF-8, a byte that is not UTF-8 replaced rather than refused.
        try (var reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            var entries = PropertiesUtils.readEntries(reader, true);
            var texts = new LinkedHashMap<String, @Nullable String>();
            entries.forEach(entry -> texts.put(entry.key(), entry.value()));
            return Optional.of(new Language(tag, file, entries, texts));
        } catch (IOException e) {
            log.warn("Failed to read the localization file '{}'", file, e);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            // The engine cannot read the file either: a rule finds none of its texts.
            messages.add(new OpenLFileMessage("A \\u not followed by four hexadecimal digits breaks the file: "
                    + "none of its texts is read. Write \\\\u for a backslash before u.", file, 1, null, tag));
            return Optional.empty();
        }
    }

    /** Checks every text of a language: written twice, broken, other placeholders, no default text. */
    private static void check(Language language,
                              @Nullable Language defaultLanguage,
                              OpenLMessageSource source,
                              List<OpenLMessage> messages) {
        var seen = new LinkedHashMap<String, Integer>();
        for (var entry : language.entries()) {
            var first = seen.putIfAbsent(entry.key(), entry.line());
            if (first != null) {
                messages.add(warn(language, entry, "Written twice, on lines %d and %d: the last text wins."
                        .formatted(first, entry.line())));
            }
            checkText(language, entry, defaultLanguage, source, messages);
        }
    }

    private static void checkText(Language language,
                                  PropertiesUtils.Entry entry,
                                  @Nullable Language defaultLanguage,
                                  OpenLMessageSource source,
                                  List<OpenLMessage> messages) {
        var text = entry.value();
        if (text == null) {
            // The line gives no text, and none of a dependency in the language stands in for it.
            if (!source.getMessageBundle(language.locale()).has(entry.key())) {
                messages.add(warn(language, entry, """
                        No '=' or ':' on the line, so there is no text and the rule returns the key. Write '%s =' \
                        for an empty text."""
                        .formatted(entry.key())));
            }
            return;
        }
        var pattern = MessagePattern.of(text);
        pattern.problems()
                .stream()
                .collect(Collectors.groupingBy(problem -> new Told(problem.kind(), piece(text, problem)),
                        LinkedHashMap::new,
                        Collectors.toList()))
                .values()
                .forEach(same -> messages.add(warn(language, entry, text, same)));
        var defaults = source.getMessageBundle(Locale.ROOT);
        if (language.tag().isEmpty()) {
            return;
        }
        // The default text of a dependency or a library stands in for the one the project lacks.
        var ownDefault = defaultLanguage == null ? null : defaultLanguage.texts().get(entry.key());
        var defaultText = ownDefault != null || !defaults.has(entry.key()) ? ownDefault : defaults.msg(entry.key());
        if (defaultText == null) {
            messages.add(warn(language, entry,
                    "No text in the default language: a language without a text of its own returns the key."));
            return;
        }
        // A pattern that hides its placeholders, broken or quoted, has none to compare: what it is told says enough.
        var defaultPattern = MessagePattern.of(defaultText);
        if (pattern.readsPlaceholders() && defaultPattern.readsPlaceholders()
                && !defaultPattern.indices().equals(pattern.indices())) {
            messages.add(warn(language, entry, "Placeholders %s differ from %s of the default language."
                    .formatted(pattern.indices(), defaultPattern.indices())));
        }
    }

    /**
     * What a broken pattern does when the rule reads the message. The piece of the text and the piece written in
     * ASCII are to be filled in, in this order.
     */
    private static String summaryOf(MessagePattern.Kind kind) {
        return switch (kind) {
            case BRACE -> "A brace is left open: '%s'. The rule fails when values are passed.";
            case FULL_WIDTH ->
                    "Full-width '%s' instead of '%s' in a placeholder. The rule fails when values are passed.";
            case PLACEHOLDER_NUMBER ->
                    "The placeholder '%s' does not begin with a number. The rule fails when values are passed.";
            case PLACEHOLDER_TYPE -> """
                    Unknown placeholder type '%s': use number, date, time or choice. The rule fails when values are \
                    passed.""";
            case PLACEHOLDER_FORMAT ->
                    "The placeholder format '%s' does not fit its type. The rule fails when values are passed.";
            case APOSTROPHE -> """
                    The single quotes around %s are dropped when values are passed, so the text is printed \
                    without these apostrophes. Use two single quotes ('') to represent an apostrophe.""";
            case QUOTED_PLACEHOLDER -> """
                    The single quotes around %s are dropped when values are passed, and the placeholder between them \
                    is printed as written instead of its value. Use two single quotes ('') to represent an \
                    apostrophe.""";
            case OPEN_APOSTROPHE -> """
                    An unmatched single quote causes the remaining text to be treated as a literal, preventing \
                    placeholders from being substituted. Use two single quotes ('') to represent an apostrophe.""";
            case TRAILING_APOSTROPHE -> """
                    The single quote in %s is dropped when values are passed, so the text is printed without this \
                    apostrophe. Use two single quotes ('') to represent an apostrophe.""";
            case DOUBLED_APOSTROPHE -> """
                    Without placeholders the text is printed as written, so %s shows as two single quotes. Use one \
                    single quote (') to represent an apostrophe.""";
        };
    }

    /** Counts the keys of the default language the language has no text for, nor any of its parent languages. */
    private static void checkTranslated(Language language,
                                        Language defaultLanguage,
                                        Map<String, Language> languages,
                                        List<OpenLMessage> messages) {
        var chain = new ArrayList<Language>();
        chain.add(language);
        language.parents()
                .stream()
                .map(languages::get)
                .filter(Objects::nonNull)
                .forEach(chain::add);
        // A key the default language writes without a text has nothing to translate.
        var untranslated = defaultLanguage.texts()
                .entrySet()
                .stream()
                .filter(text -> text.getValue() != null)
                .map(Map.Entry::getKey)
                .filter(key -> chain.stream().noneMatch(step -> step.texts().containsKey(key)))
                .count();
        if (untranslated > 0) {
            var summary = untranslated == 1
                    ? "1 key is not translated into %s: the default text is shown."
                    : untranslated + " keys are not translated into %s: the default texts are shown.";
            messages.add(new OpenLFileMessage(summary.formatted(language.name()),
                    language.file(), 1, null, language.tag()));
        }
    }

    /** A warning about the text of a key as a whole, which an editor of the file marks on the key. */
    private static OpenLMessage warn(Language language, PropertiesUtils.Entry entry, String summary) {
        var key = entry.places().ofKey();
        return new OpenLFileMessage(summary, language.file(), entry.line(), entry.key(), language.tag(), List.of(),
                key == null ? List.of() : List.of(key));
    }

    /**
     * A warning about a problem of the text of a key, which tells every place it is in. The problems are of one kind
     * and of the same piece of text.
     */
    private static OpenLMessage warn(Language language,
                                     PropertiesUtils.Entry entry,
                                     String text,
                                     List<MessagePattern.Problem> same) {
        var first = same.getFirst();
        var piece = piece(text, first);
        var summary = summaryOf(first.kind()).formatted(piece, MessagePattern.asciiOf(piece));
        var uri = language.file().toUri().toString();
        var pieces = same.stream()
                .map(problem -> new SourceRange(uri, text, problem.start(), problem.end()))
                .toList();
        var places = same.stream()
                .map(problem -> entry.places().ofValue(problem.start(), problem.end()))
                .filter(Objects::nonNull)
                .toList();
        return new OpenLFileMessage(summary, language.file(), entry.line(), entry.key(), language.tag(), pieces,
                places);
    }

    /** What a warning tells: a kind of problem and the piece of text it is in, wherever that piece is. */
    private record Told(MessagePattern.Kind kind, String piece) {
    }

    private static String piece(String text, MessagePattern.Problem problem) {
        return text.substring(problem.start(), problem.end());
    }
}
