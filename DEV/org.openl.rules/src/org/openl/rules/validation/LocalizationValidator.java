package org.openl.rules.validation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.engine.OpenLCompileManager;
import org.openl.message.OpenLFileMessage;
import org.openl.message.OpenLMessage;
import org.openl.rules.binding.OpenLMessageSource;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenClass;
import org.openl.util.PropertiesUtils;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;

/**
 * Checks the localization files of a project: the {@code message*.properties} files of its {@code i18n} folder.
 *
 * <p>Every problem is a warning, as a rule asking for a missing or broken message still runs. A text is reported
 * when its key is written twice in a file, when its pattern is broken, when its placeholders differ from those of
 * the default language, and when the default language has no text for its key. A language that leaves keys of the
 * default language untranslated gets one warning with their count; a regional language that inherits the text of
 * its parent language translates it.
 *
 * <p>A key is read as the engine reads it: anything before the first equals sign or colon, spaces inside it
 * included, such as {@code My some key = Some text}. A line without either has a key and no text, not an empty
 * one: a rule asking for it gets the key itself, which is reported too.
 *
 * <p>Only the files of the project itself are checked, once per compilation of the whole project. A key the
 * default files of a dependency or a library hold is not missing, and its translations take their placeholders
 * from that text.
 */
@Slf4j
public final class LocalizationValidator implements IOpenLValidator {

    private static final String FOLDER = "i18n";
    private static final Pattern FILE = Pattern.compile("message(?:_([^.]+))?\\.properties");

    /**
     * One localization file of the project: its language and what it writes, line by line. A key written on a
     * line without a separator has a null text: a rule asking for it gets the key itself.
     */
    private record Language(String tag,
                            Path file,
                            List<PropertiesUtils.Entry> entries,
                            Map<String, @Nullable String> texts) {

        String name() {
            return tag.isEmpty() ? "the default language" : Locale.forLanguageTag(tag.replace('_', '-'))
                    .getDisplayName(Locale.ENGLISH);
        }

        String fileName() {
            return file.getFileName().toString();
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

    @Override
    public ValidationResult validate(IOpenClass openClass) {
        if (!(openClass instanceof XlsModuleOpenClass module)) {
            return ValidationUtils.validationSuccess();
        }
        var folder = module.getRulesModuleBindingContext()
                .getExternalParams()
                .get(OpenLCompileManager.PROJECT_FOLDER_KEY);
        if (!(folder instanceof Path project)) {
            return ValidationUtils.validationSuccess();
        }
        var languages = read(project.resolve(FOLDER));
        var messages = new ArrayList<OpenLMessage>();
        var defaults = module.getMessageSource().getMessageBundle(Locale.ROOT);
        var defaultLanguage = languages.get("");
        languages.values().forEach(language -> check(language, defaultLanguage, defaults, messages));
        if (defaultLanguage != null) {
            languages.values().stream()
                    .filter(language -> !language.tag().isEmpty())
                    .forEach(language -> checkTranslated(language, defaultLanguage, languages, messages));
        }
        return ValidationUtils.withMessages(messages);
    }

    /** The localization files of the folder by their tags, the default one first. */
    private static Map<String, Language> read(Path folder) {
        var languages = new LinkedHashMap<String, Language>();
        if (!Files.isDirectory(folder)) {
            return languages;
        }
        try (Stream<Path> files = Files.list(folder)) {
            files.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::getFileName))
                    .forEach(file -> readLanguage(file).ifPresent(language -> languages.put(language.tag(), language)));
        } catch (IOException e) {
            log.warn("Failed to list the localization files of '{}'", folder, e);
        }
        return languages;
    }

    private static Optional<Language> readLanguage(Path file) {
        var matcher = FILE.matcher(file.getFileName().toString());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        var tag = matcher.group(1) == null ? "" : matcher.group(1);
        // Read as the engine reads it: in UTF-8, a byte that is not UTF-8 replaced rather than refused.
        try (var reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            var entries = PropertiesUtils.readEntries(reader);
            var texts = new LinkedHashMap<String, @Nullable String>();
            entries.forEach(entry -> texts.put(entry.key(), entry.value()));
            return Optional.of(new Language(tag, file, entries, texts));
        } catch (IOException e) {
            log.warn("Failed to read the localization file '{}'", file, e);
            return Optional.empty();
        }
    }

    /** Checks every text of a language: written twice, broken, other placeholders, no default text. */
    private static void check(Language language,
                              @Nullable Language defaultLanguage,
                              OpenLMessageSource.MessageBundle defaults,
                              List<OpenLMessage> messages) {
        var seen = new LinkedHashMap<String, Integer>();
        for (var entry : language.entries()) {
            var first = seen.putIfAbsent(entry.key(), entry.line());
            if (first != null) {
                messages.add(warn(language, entry, """
                        Message key '%s' is written twice in '%s', on lines %d and %d: the last text wins."""
                        .formatted(entry.key(), language.fileName(), first, entry.line())));
            }
            checkText(language, entry, defaultLanguage, defaults, messages);
        }
    }

    private static void checkText(Language language,
                                  PropertiesUtils.Entry entry,
                                  @Nullable Language defaultLanguage,
                                  OpenLMessageSource.MessageBundle defaults,
                                  List<OpenLMessage> messages) {
        var text = entry.value();
        if (text == null) {
            messages.add(warn(language, entry, """
                    Message key '%s' in '%s' has no '=' or ':' on its line, so it has no text and the rule returns \
                    the key itself. Write '%s =' for an empty text."""
                    .formatted(entry.key(), language.fileName(), entry.key())));
            return;
        }
        var pattern = MessagePattern.of(text);
        if (pattern.problem() == MessagePattern.Problem.BRACE) {
            messages.add(warn(language, entry, ("""
                    Message '%s' in '%s' has a brace left open or a placeholder that is \
                    not a number: it cannot take values.""").formatted(entry.key(), language.fileName())));
        } else if (pattern.problem() == MessagePattern.Problem.APOSTROPHE) {
            messages.add(warn(language, entry, ("""
                    Message '%s' in '%s' has an apostrophe left open: when values are \
                    passed, the rest of the text is printed as it is. Write '' for an apostrophe.""")
                    .formatted(entry.key(), language.fileName())));
        }
        if (language.tag().isEmpty()) {
            return;
        }
        // The default text of a dependency or a library stands in for the one the project lacks.
        var ownDefault = defaultLanguage == null ? null : defaultLanguage.texts().get(entry.key());
        var defaultText = ownDefault != null || !defaults.has(entry.key()) ? ownDefault : defaults.msg(entry.key());
        if (defaultText == null) {
            messages.add(warn(language, entry, ("""
                    Message key '%s' is translated in '%s' but has no text in the default \
                    language: every other language returns the key itself.""")
                    .formatted(entry.key(), language.fileName())));
        } else if (defaultText != null && !MessagePattern.of(defaultText).indices().equals(pattern.indices())) {
            messages.add(warn(language, entry, """
                    Placeholders of message '%s' in '%s' %s differ from the default language %s."""
                    .formatted(entry.key(), language.fileName(), pattern.indices(),
                            MessagePattern.of(defaultText).indices())));
        }
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
                    ? "1 message key is not translated into %s ('%s'): its customers get the default text."
                    : untranslated
                    + " message keys are not translated into %s ('%s'): its customers get the default texts.";
            messages.add(new OpenLFileMessage(summary.formatted(language.name(), language.fileName()),
                    language.file(), 1, null, language.tag()));
        }
    }

    private static OpenLMessage warn(Language language, PropertiesUtils.Entry entry, String summary) {
        return new OpenLFileMessage(summary, language.file(), entry.line(), entry.key(), language.tag());
    }
}
