package org.openl.studio.tags.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.tags.model.TagFillPreview;
import org.openl.studio.tags.model.TagFillPreview.TagFillItem;
import org.openl.studio.tags.model.TagFillState;

/**
 * Assigns tags derived from the project name templates to the projects that are missing them.
 *
 * <p>The same reading of a template answers both questions: what filling would do to a project, and what
 * it does when the user asks for it.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TagFillService {

    private final TagTemplateService tagTemplateService;
    private final TagCatalogProvider tagCatalogProvider;
    private final TagAssignmentValidator tagAssignmentValidator;
    private final AclProjectsHelper aclProjectsHelper;

    @Lookup
    public UserWorkspace getUserWorkspace() {
        return null;
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
     * @param projectNames business names of the projects to fill, empty for all of them
     * @return how many projects were updated and how many were left alone
     */
    public Map<String, Integer> fill(@Nullable Collection<String> projectNames) {
        var workspace = getUserWorkspace();
        var updated = 0;
        var skipped = 0;
        for (RulesProject project : workspace.getProjects()) {
            try {
                var tags = requestedTags(project, projectNames);
                if (tags.isEmpty()) {
                    skipped++;
                    continue;
                }
                // Template tags take priority over what the project carries.
                var currentTags = new HashMap<String, String>(project.getLocalTags());
                currentTags.putAll(tags);
                project.saveTags(currentTags);
                updated++;
            } catch (Exception e) {
                log.warn("Failed to fill tags for project '{}'", project.getBusinessName(), e);
                skipped++;
            }
        }
        workspace.refresh();
        return Map.of("updated", updated, "skipped", skipped);
    }

    /** The tags to assign to the project, empty when it was not asked for or nothing can be assigned. */
    private Map<String, String> requestedTags(RulesProject project, @Nullable Collection<String> projectNames) {
        if (projectNames != null && !projectNames.isEmpty() && !projectNames.contains(project.getBusinessName())) {
            return Map.of();
        }
        return tagAssignmentValidator.applicable(derivedTags(project));
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
        // Filling writes the tags file whether the project is opened or not, so only the write
        // permission decides whether the row can be picked.
        var modifiable = aclProjectsHelper.hasPermission(project, BasePermission.WRITE);
        return Optional.of(new TagFillPreview(project.getBusinessName(), modifiable, items));
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
    @Nullable
    private static String valueOf(Map<String, String> tags, String typeName) {
        return tags.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(typeName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
