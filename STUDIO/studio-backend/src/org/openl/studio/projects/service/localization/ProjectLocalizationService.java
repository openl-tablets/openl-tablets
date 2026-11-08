package org.openl.studio.projects.service.localization;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

import org.openl.rules.binding.OpenLMessageSource;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.validation.LocalizationCheck;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.localization.LocalizationAction;
import org.openl.studio.projects.model.localization.LocalizationKeyView;
import org.openl.studio.projects.model.localization.LocalizationLanguageView;
import org.openl.studio.projects.model.localization.LocalizationMessageView;
import org.openl.studio.projects.model.localization.LocalizationMessagesPage;
import org.openl.studio.projects.model.localization.LocalizationSharedValueView;
import org.openl.studio.projects.model.localization.LocalizationWarningView;
import org.openl.studio.projects.model.project.status.LocalizationFileMessageSource;
import org.openl.studio.projects.model.project.status.MessagePositions;
import org.openl.studio.projects.service.DetailedMessageDescriptionMapper;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.project.compile.CompilationJob;
import org.openl.studio.projects.service.project.compile.CompilationJobRegistry;

/**
 * The messages of an open project: read by the model of the session once and kept, served by pages of keys, and
 * written by actions.
 *
 * <p>A project nobody opened has no files in the workspace to read, and is refused.
 *
 * <p>A write changes only what it must: a text written over the key where it stands, a key added at the end of a
 * file. The compiled project reads the new texts at once, and its warnings about them are told again, without
 * compiling anything.
 */
@Service
@RequiredArgsConstructor
public class ProjectLocalizationService {

    private static final Pattern LANGUAGE = Pattern.compile(LocalizationAction.LANGUAGE);

    private final WorkspaceProjectService workspaceProjectService;
    private final DetailedMessageDescriptionMapper messageMapper;
    private final ProjectFileRootFactory fileRootFactory;
    private final CompilationJobRegistry compilationJobRegistry;
    /** Makes the writes of the localization files one at a time across the sessions: each is short. */
    private final ReentrantLock writes = new ReentrantLock();

    @Lookup
    public WebStudio getWebStudio() {
        // Spring overrides this method with a lookup of the bean; the stub itself never runs.
        throw new UnsupportedOperationException("Overridden by the Spring @Lookup container");
    }

    /**
     * Every key of the project, in the order they are written: those of the default file first, then the rest.
     *
     * @param project the project
     * @return the keys
     */
    public List<LocalizationKeyView> keys(RulesProject project) {
        var descriptor = workspaceProjectService.getProjectDescriptor(project);
        return messagesOf(descriptor).keys().stream().map(LocalizationKeyView::new).toList();
    }

    /**
     * A page of the keys the query leaves, each with its texts in every language and what the check of the files
     * warns about them.
     *
     * @param project the project
     * @param query   what the keys are narrowed down to
     * @param key     a key to answer alone, whatever the query; {@code null} for the keys the query leaves
     * @param page    the page
     * @return the page, with the languages of the project
     */
    public LocalizationMessagesPage messages(RulesProject project,
                                             LocalizationQuery query,
                                             @Nullable String key,
                                             Pageable page) {
        var read = read(project);
        var messages = read.messages();
        var tags = sharedTags(messages);
        var keys = key == null ? query.keysOf(messages, read.warnings().keySet()) : keyAlone(messages, key, tags);
        var from = page.isUnpaged() ? 0 : Math.min(page.getOffset(), keys.size());
        var to = page.isUnpaged() ? keys.size() : Math.min(from + page.getPageSize(), keys.size());
        var ids = new SharedIds(project, messages);
        var content = keys.subList(from, to)
                .stream()
                .map(k -> rowOf(messages, k, tags, read.warnings(), ids))
                .toList();
        var languages = messages.files()
                .stream()
                .map(file -> new LocalizationLanguageView(file.tag(), file.legacy() ? Boolean.TRUE : null))
                .toList();
        return new LocalizationMessagesPage(languages, content, page, keys.size());
    }

    /**
     * Makes the changes in order, then writes every file they changed at once. A change that is refused refuses them
     * all, and no file is written.
     *
     * <p>The changes of a project are written one at a time, whichever session sends them. Every file changed is
     * written beside itself first, and only once all of them are written do they take the places of the files: a
     * failure before then changes nothing, and a file that changed since it was read refuses the changes.
     *
     * @param project the project
     * @param actions the changes, in order
     */
    public void apply(RulesProject project, List<LocalizationAction> actions) {
        fileRootFactory.of(project).requireModifiable();
        var descriptor = workspaceProjectService.getProjectDescriptor(project);
        writes.lock();
        try {
            write(project, descriptor, actions);
        } finally {
            writes.unlock();
        }
        project.setModified();
        // The files are written past the tree of the project, which is read again so it lists a new one.
        project.refresh();
        compilationJobRegistry.find(project.getProjectId(), project.getBranch())
                .map(CompilationJob::project)
                .ifPresent(ProjectModel::checkLocalizationAgain);
    }

    private void write(RulesProject project, ProjectDescriptor descriptor, List<LocalizationAction> actions) {
        var messages = messagesOf(descriptor);
        var edits = new Edits(messages.files(), descriptor.getProjectFolder().resolve(OpenLMessageSource.FOLDER));
        actions.forEach(edits::make);
        lock(project);
        var changed = edits.changed();
        var written = new ArrayList<MessageFileEdit.Aside>();
        try {
            for (var edit : changed) {
                written.add(edit.writeAside());
            }
            // A file another writer changed while the others were written is not written over either.
            changed.forEach(MessageFileEdit::requireUnchanged);
            MessageFileEdit.replaceAll(written);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            written.forEach(MessageFileEdit.Aside::discard);
            var cache = getWebStudio().getModel().getLocalizationCache();
            changed.forEach(edit -> cache.forget(edit.path()));
        }
    }

    /** What the session keeps about the messages of a project, and the warnings about them by the key. */
    private record Read(ProjectMessages messages, Map<String, List<LocalizationWarningView>> warnings) {
    }

    private Read read(RulesProject project) {
        var descriptor = workspaceProjectService.getProjectDescriptor(project);
        var messages = messagesOf(descriptor);
        var problems = messages.problems(() -> workspaceProjectService.checkLocalization(descriptor));
        var warnings = new HashMap<String, List<LocalizationWarningView>>();
        messageMapper.mapFileMessages(problems, descriptor.getProjectFolder(), MessagePositions.TEXT)
                .forEach(message -> {
                    if (message.location() instanceof LocalizationFileMessageSource source && source.key() != null) {
                        warnings.computeIfAbsent(source.key(), k -> new ArrayList<>())
                                .add(new LocalizationWarningView(source.language(),
                                        message.source().summary(),
                                        source.spots()));
                    }
                });
        return new Read(messages, warnings);
    }

    private ProjectMessages messagesOf(ProjectDescriptor descriptor) {
        return getWebStudio().getModel()
                .getLocalizationCache()
                .messagesOf(descriptor, workspaceProjectService.localizationDependenciesOf(descriptor));
    }

    private static LocalizationMessageView rowOf(ProjectMessages messages,
                                                 String key,
                                                 Set<String> tags,
                                                 Map<String, List<LocalizationWarningView>> warnings,
                                                 SharedIds ids) {
        var shared = new LinkedHashMap<String, LocalizationSharedValueView>();
        tags.forEach(tag -> messages.sharedTextOf(tag, key).ifPresent(text -> {
            var project = Objects.requireNonNullElse(text.project(), "");
            shared.put(tag, new LocalizationSharedValueView(text.text(), project, ids.of(project)));
        }));
        return new LocalizationMessageView(key, messages.textsOf(key), shared, warnings.getOrDefault(key, List.of()));
    }

    /** The ids of the projects the texts come from, found the first time a text of one of them is shown. */
    private final class SharedIds {

        private final RulesProject project;
        private final ProjectMessages messages;
        private @Nullable Map<Path, String> byFolder;

        SharedIds(RulesProject project, ProjectMessages messages) {
            this.project = project;
            this.messages = messages;
        }

        @Nullable
        String of(String dependency) {
            if (byFolder == null) {
                byFolder = workspaceProjectService.dependencyIdsOf(project);
            }
            var ids = byFolder;
            return messages.folderOf(dependency).map(ids::get).orElse(null);
        }
    }

    /**
     * A key asked for alone: one the project writes, or one only a project it depends on gives, which the project
     * reads from there.
     */
    private static List<String> keyAlone(ProjectMessages messages, String key, Set<String> tags) {
        var given = messages.has(key) || tags.stream().anyMatch(tag -> messages.sharedTextOf(tag, key).isPresent());
        return given ? List.of(key) : List.of();
    }

    /** The languages of the project with every language they fall back to: a dependency may give a text in any. */
    private static Set<String> sharedTags(ProjectMessages messages) {
        var tags = new LinkedHashSet<String>();
        messages.files().forEach(file -> tags.addAll(ProjectMessages.withParents(file.tag())));
        return tags;
    }

    private static void lock(RulesProject project) {
        try {
            project.tryLockOrThrow();
        } catch (ProjectException e) {
            throw new ConflictException("project.locked.message");
        }
    }

    /** The changes of the files of the languages, made one after another before anything is written. */
    private static final class Edits {

        private final Path folder;
        private final Map<String, MessageFileEdit> byLanguage = new LinkedHashMap<>();

        Edits(List<MessageFile> files, Path folder) {
            this.folder = folder;
            files.forEach(file -> byLanguage.put(file.tag(), new MessageFileEdit(file)));
        }

        void make(LocalizationAction action) {
            switch (action) {
                case LocalizationAction.SetValue(var key, var language, var value) -> of(language).set(key, value);
                case LocalizationAction.RemoveValue(var key, var language) -> removeValue(key, language);
                case LocalizationAction.AddKey(var key, var values) -> addKey(key, values);
                case LocalizationAction.RemoveKey(var key) -> removeKey(key);
                case LocalizationAction.AddLanguage(var language, var values) -> addLanguage(language, values);
            }
        }

        List<MessageFileEdit> changed() {
            return byLanguage.values().stream().filter(MessageFileEdit::changed).toList();
        }

        private MessageFileEdit of(String language) {
            return Optional.ofNullable(byLanguage.get(language))
                    .orElseThrow(() -> new NotFoundException("localization.language.not-found.message", language));
        }

        private void removeValue(String key, String language) {
            var edit = of(language);
            if (!edit.has(key)) {
                throw new NotFoundException("localization.key.not-found.message", key);
            }
            edit.remove(key);
        }

        private void addKey(String key, Map<String, String> values) {
            if (byLanguage.values().stream().anyMatch(edit -> edit.has(key))) {
                throw new ConflictException("localization.key.exists.message", key);
            }
            values.keySet().forEach(Edits::requireLanguage);
            values.forEach((language, value) -> of(language).set(key, value));
        }

        private void removeKey(String key) {
            var holding = byLanguage.values().stream().filter(edit -> edit.has(key)).toList();
            if (holding.isEmpty()) {
                throw new NotFoundException("localization.key.not-found.message", key);
            }
            holding.forEach(edit -> edit.remove(key));
        }

        private void addLanguage(String language, @Nullable Map<String, String> values) {
            requireLanguage(language);
            if (byLanguage.containsKey(language)) {
                throw new ConflictException("localization.language.exists.message", language);
            }
            var edit = new MessageFileEdit(folder.resolve(fileOf(language)));
            if (values != null) {
                values.forEach(edit::set);
            }
            byLanguage.put(language, edit);
        }

        /** A language written as the engine names the file of its locale: the language lower, the country upper. */
        private static void requireLanguage(String language) {
            if (!LANGUAGE.matcher(language).matches()
                    || !language.equals(LocalizationCheck.localeOf(language).toString())) {
                throw new BadRequestException("localization.language.invalid.message", new Object[]{language});
            }
        }

        private static String fileOf(String language) {
            return language.isEmpty() ? "message.properties" : "message_" + language + ".properties";
        }
    }
}
