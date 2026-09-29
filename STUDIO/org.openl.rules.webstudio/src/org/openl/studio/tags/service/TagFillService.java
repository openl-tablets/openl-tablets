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
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.tags.model.TagFillPreview;
import org.openl.studio.tags.model.TagFillPreview.TagFillItem;
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
     * Assigns the derived tags to the given projects, or to every project when no name is given.
     *
     * <p>A value the templates derived for a tag type that does not take it is left out, so one tag that
     * cannot be assigned does not cost the project the others.
     *
     * <p>A project the current user cannot change now is left alone, and no tag value is created for it. A
     * project that would not change is left alone too.
     *
     * @param projectNames business names of the projects to fill, empty for all of them
     * @return how many projects were updated and how many were left alone
     */
    public Map<String, Integer> fill(@Nullable Collection<String> projectNames) {
        var workspace = getUserWorkspace();
        var updated = 0;
        var skipped = 0;
        for (RulesProject project : workspace.getProjects()) {
            if (fillProject(project, projectNames)) {
                updated++;
            } else {
                skipped++;
            }
        }
        if (updated > 0) {
            workspace.refresh();
        }
        return Map.of("updated", updated, "skipped", skipped);
    }

    /** Writes the derived tags into the tags file of the project, and answers whether it did. */
    private boolean fillProject(RulesProject project, @Nullable Collection<String> projectNames) {
        try {
            var tags = tagsToWrite(project, projectNames);
            if (tags.isEmpty()) {
                return false;
            }
            projectFileRootFactory.of(project)
                    .writeBatch("", List.of(tagsFile(tags)), ChangesetType.DIFF, FILL_COMMENT);
            return true;
        } catch (Exception e) {
            log.warn("Failed to fill tags for project '{}'", project.getBusinessName(), e);
            return false;
        }
    }

    /**
     * The tags the tags file of the project gets: the ones it carries, with the derived values assigned.
     *
     * <p>Empty when the project was not asked for, cannot be changed now, or would not change.
     */
    private Map<String, String> tagsToWrite(RulesProject project, @Nullable Collection<String> projectNames) {
        var derived = isRequested(project, projectNames) ? derivedTags(project) : Map.<String, String>of();
        if (derived.isEmpty() || !isFillable(project)) {
            return Map.of();
        }
        var assigned = project.getLocalTags();
        // Template tags take priority over what the project carries. New values are created in the catalog
        // only for a project that takes them.
        var tags = new LinkedHashMap<String, String>(assigned);
        tags.putAll(tagAssignmentValidator.applicable(derived));
        return tags.equals(assigned) ? Map.of() : tags;
    }

    private static boolean isRequested(RulesProject project, @Nullable Collection<String> projectNames) {
        return projectNames == null || projectNames.isEmpty() || projectNames.contains(project.getBusinessName());
    }

    private Optional<TagFillPreview> previewOf(RulesProject project, TagCatalog catalog) {
        var derived = derivedTags(project);
        if (derived.isEmpty()) {
            return Optional.empty();
        }
        var assigned = project.getLocalTags();
        var items = derived.entrySet().stream()
                .map(entry -> item(catalog, assigned, entry.getKey(), entry.getValue()))
                .toList();
        if (items.stream().allMatch(item -> item.state() == TagFillState.KEEP)) {
            return Optional.empty();
        }
        return Optional.of(new TagFillPreview(project.getBusinessName(), isFillable(project), items));
    }

    /**
     * Whether filling can write the tags file of the project now.
     *
     * <p>The current user must be able to change the project now, as for any write into its files.
     *
     * <p>A closed project is written straight to its repository. A repository that keeps each project as an
     * archive cannot take a single file, so such a project takes the tags only once it is opened. An older
     * revision opened to be read is left alone as well: the first write into it is the reader's decision.
     */
    private boolean isFillable(RulesProject project) {
        // The state of the project is weighed first; the files mount asks for the write permission last.
        return project.isFolder() && !project.isReadingOtherVersion()
                && projectFileRootFactory.of(project).isModifiable();
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
        return catalog.type(typeName).filter(TagType::isExtensible).isPresent()
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
