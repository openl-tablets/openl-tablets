package org.openl.studio.tags.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

import org.openl.rules.project.abstraction.ProjectTags;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.validator.ModifyRestrictedException;
import org.openl.studio.projects.validator.ModifyRestriction;
import org.openl.studio.tags.model.TagFillBlocker;
import org.openl.studio.tags.model.TagFillOutcome;
import org.openl.studio.tags.model.TagFillPreview;
import org.openl.studio.tags.model.TagFillPreview.TagFillItem;
import org.openl.studio.tags.model.TagFillResult;
import org.openl.studio.tags.model.TagFillState;
import org.openl.util.PropertiesUtils;

/**
 * Assigns tags derived from the project name templates to the projects that are missing them.
 *
 * <p>The same reading of a template answers both questions: what filling would do to a project, and what
 * it does when the user asks for it.
 *
 * <p>The tags file is written the way the project files are. A closed project gets it in a commit to the
 * design repository on behalf of the current user. An opened project gets it in its working copy, where it
 * waits to be saved.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TagFillService {

    /** The commit message of the tags file a closed project gets. */
    static final String FILL_COMMENT = "Fill tags from the project name templates";

    private final TagTemplateService tagTemplateService;
    private final TagCatalogProvider tagCatalogProvider;
    private final TagAssignmentValidator tagAssignmentValidator;
    private final ProjectFileRootFactory projectFileRootFactory;

    @Lookup
    public UserWorkspace getUserWorkspace() {
        // Spring overrides this method with a lookup of the bean; the stub itself never runs.
        throw new UnsupportedOperationException("Overridden by the Spring @Lookup container");
    }

    /**
     * The projects a template names a tag for that they do not carry yet, with what filling would do to
     * each of their tags.
     *
     * <p>A project whose tags already match its templates is left out: there is nothing to show for it.
     */
    public List<TagFillPreview> preview() {
        var catalog = tagCatalogProvider.get();
        var previews = new ArrayList<TagFillPreview>();
        for (RulesProject project : getUserWorkspace().getProjects()) {
            try {
                previewOf(project, catalog).ifPresent(previews::add);
            } catch (Exception e) {
                log.warn("Failed to read the tags of project '{}'", project.getBusinessName(), e);
            }
        }
        return previews;
    }

    /**
     * Assigns the derived tags to the given projects, or to every project when no name is given, and answers
     * what it did to each of them.
     *
     * <p>A value the templates derived for a tag type that does not take it is left out, so one tag that
     * cannot be assigned does not cost the project the others.
     *
     * <p>A project the current user cannot change now is left alone, and no tag value is created for it. So
     * is a project none of whose missing values can be assigned. Each project left alone is reported with the
     * reason, and so is every missing value a project could not get.
     *
     * <p>A project that already carries every value the templates derive for it is not reported: there is
     * nothing to fill in it, as in {@link #preview()}.
     *
     * @param projectNames business names of the projects to fill, empty for all of them
     * @return what filling did to each project it was asked for that a template derives a missing value for
     */
    public List<TagFillResult> fill(@Nullable Collection<String> projectNames) {
        var workspace = getUserWorkspace();
        var results = new ArrayList<TagFillResult>();
        for (RulesProject project : workspace.getProjects()) {
            if (isRequested(project, projectNames)) {
                fillProject(project).ifPresent(results::add);
            }
        }
        if (results.stream().anyMatch(result -> result.outcome() == TagFillOutcome.UPDATED)) {
            workspace.refresh();
        }
        return results;
    }

    private static boolean isRequested(RulesProject project, @Nullable Collection<String> projectNames) {
        return projectNames == null || projectNames.isEmpty() || projectNames.contains(project.getBusinessName());
    }

    /** What filling does to the project, empty when the project carries every value the templates derive. */
    private Optional<TagFillResult> fillProject(RulesProject project) {
        try {
            var derived = derivedTags(project);
            // The tags of a project no template matches are not read at all.
            var assigned = derived.isEmpty() ? Map.<String, String>of() : project.getLocalTags();
            var missing = changes(assigned, derived);
            return missing.isEmpty() ? Optional.empty() : Optional.of(resultOf(project, assigned, missing));
        } catch (Exception e) {
            log.warn("Failed to fill tags for project '{}'", project.getBusinessName(), e);
            return Optional.of(TagFillResult.failed(project.getBusinessName()));
        }
    }

    /** Writes the missing values the project can take into its tags file, when it can be changed now. */
    private TagFillResult resultOf(RulesProject project,
                                   Map<String, String> assigned,
                                   Map<String, String> missing) throws IOException {
        var blocker = blockerOf(project);
        if (blocker.isPresent()) {
            return TagFillResult.notModifiable(project.getBusinessName(), blocker.get());
        }
        // New values are created in the catalog only for a project that takes them.
        var changes = tagAssignmentValidator.applicable(missing);
        var rejected = new LinkedHashMap<>(missing);
        rejected.keySet().removeAll(changes.keySet());
        if (changes.isEmpty()) {
            return TagFillResult.nothingToAssign(project.getBusinessName(), rejected);
        }
        write(project, assigned, changes);
        return TagFillResult.updated(project.getBusinessName(), changes, rejected);
    }

    private void write(RulesProject project, Map<String, String> assigned, Map<String, String> changes)
            throws IOException {
        // Template tags take priority over what the project carries, whatever case it spelled their types in: a
        // value is replaced where it stands, under the spelling of its tag type.
        var tags = new LinkedHashMap<String, String>();
        assigned.forEach((type, value) -> tags.put(spellingIn(changes, type), value));
        tags.putAll(changes);
        projectFileRootFactory.of(project).writeBatch("", List.of(tagsFile(tags)), ChangesetType.DIFF, FILL_COMMENT);
    }

    /** How the changes spell the tag type, or the given spelling when they do not change that tag type. */
    private static String spellingIn(Map<String, String> changes, String type) {
        return changes.keySet().stream().filter(type::equalsIgnoreCase).findFirst().orElse(type);
    }

    private Optional<TagFillPreview> previewOf(RulesProject project, TagCatalog catalog) {
        var derived = derivedTags(project);
        if (derived.isEmpty()) {
            return Optional.empty();
        }
        var assigned = project.getLocalTags();
        if (carries(assigned, derived)) {
            return Optional.empty();
        }
        var items = derived.entrySet().stream()
                .map(entry -> item(catalog, assigned, entry.getKey(), entry.getValue()))
                .toList();
        return Optional.of(TagFillPreview.of(project.getBusinessName(), blockerOf(project).orElse(null), items));
    }

    /** Whether the project carries every value the templates derive for it, whatever case it spelled them in. */
    private static boolean carries(Map<String, String> assigned, Map<String, String> derived) {
        return changes(assigned, derived).isEmpty();
    }

    /** The values the project does not carry yet, whatever case it spelled them in. */
    private static Map<String, String> changes(Map<String, String> assigned, Map<String, String> values) {
        var changes = new LinkedHashMap<String, String>();
        values.forEach((type, value) -> {
            if (!value.equalsIgnoreCase(valueOf(assigned, type))) {
                changes.put(type, value);
            }
        });
        return changes;
    }

    /**
     * Why filling cannot write the tags file of the project now, empty when it can.
     *
     * <p>The current user must be able to change the project now, as for any write into its files.
     *
     * <p>A closed project is written straight to its repository. A repository that keeps each project as an
     * archive cannot take a single file, so such a project takes the tags only once it is opened. An older
     * revision opened to be read is left alone as well: the first write into it is the reader's decision.
     */
    private Optional<TagFillBlocker> blockerOf(RulesProject project) {
        if (!project.isFolder()) {
            return Optional.of(TagFillBlocker.of(TagFillBlocker.Reason.ARCHIVE));
        }
        if (project.isReadingOtherVersion()) {
            return Optional.of(TagFillBlocker.of(TagFillBlocker.Reason.OLDER_REVISION));
        }
        try {
            projectFileRootFactory.of(project).requireModifiable();
            return Optional.empty();
        } catch (ModifyRestrictedException e) {
            return Optional.of(blockerOf(project, e.getRestriction()));
        } catch (ForbiddenException e) {
            return Optional.of(TagFillBlocker.of(TagFillBlocker.Reason.NO_PERMISSION));
        }
    }

    private static TagFillBlocker blockerOf(RulesProject project, ModifyRestriction restriction) {
        return switch (restriction) {
            case BRANCH_PROTECTED -> TagFillBlocker.branchProtected(project.getBranch());
            case LOCKED -> lockBlocker(project);
        };
    }

    /** A lock of the current user that outlived their editing is theirs to release, unlike a lock of another user. */
    private static TagFillBlocker lockBlocker(RulesProject project) {
        var lock = project.getLockInfo();
        return project.isLockedByMe(lock)
                ? TagFillBlocker.of(TagFillBlocker.Reason.LOCKED_BY_YOU)
                : TagFillBlocker.locked(lock.getLockedBy());
    }

    /** The tags file of a project carrying the given tags. */
    private static FileItem tagsFile(Map<String, String> tags) throws IOException {
        var content = new ByteArrayOutputStream();
        PropertiesUtils.store(content, tags.entrySet());
        return new FileItem(ProjectTags.TAGS_FILE_NAME, new ByteArrayInputStream(content.toByteArray()));
    }

    private TagFillItem item(TagCatalog catalog, Map<String, String> assigned, String typeName, String derived) {
        var current = valueOf(assigned, typeName);
        return new TagFillItem(typeName, current, derived, state(catalog, typeName, derived, current));
    }

    private TagFillState state(TagCatalog catalog, String typeName, String derived, @Nullable String current) {
        if (current != null && derived.equalsIgnoreCase(current)) {
            return TagFillState.KEEP;
        }
        if (catalog.configuredValue(typeName, derived).isPresent()) {
            return TagFillState.ASSIGN;
        }
        // A new value is created as filling creates it: for an extensible tag type, and only as a valid tag name.
        return catalog.type(typeName).filter(type -> TagAssignmentValidator.isCreatable(type, derived)).isPresent()
                ? TagFillState.CREATE
                : TagFillState.REJECTED;
    }

    /** The tag values the project name templates derive for the project, by tag type name. */
    private Map<String, String> derivedTags(RulesProject project) {
        var derived = tagTemplateService.getTags(project.getBusinessName());
        var tags = new LinkedHashMap<String, String>();
        derived.forEach(tag -> tags.putIfAbsent(tag.getType().getName(), tag.getName()));
        return tags;
    }

    /** The value assigned for that tag type, whatever case the project spelled the type in. */
    private static @Nullable String valueOf(Map<String, String> tags, String typeName) {
        return tags.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(typeName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
