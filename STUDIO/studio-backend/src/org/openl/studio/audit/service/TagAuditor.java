package org.openl.studio.audit.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.rest.acl.service.AclRoleAuditor.Place;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditChanges;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditTargetType;

/**
 * Announces the changes of the tags configuration to the audit log.
 *
 * <p>It covers the tag types created, changed, deleted and reordered, the tags added, renamed and deleted, the
 * project name templates changed, and the projects a fill gave their tags. A change that leaves every value as it
 * was announces nothing.
 *
 * <p>The tag types, the tags and the templates live in the database, so their events are announced in the
 * transaction of the change and are saved with it. A fill writes to the projects, which no transaction covers, so
 * it is announced once it is done, and a failure to record it is logged and never fails the fill.
 *
 * @author Yury Molchan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagAuditor {

    /** The detail that names one tag of a tag type. */
    private static final String TAG = "tag";
    /** The detail that lists the tags of a tag type. */
    private static final String TAGS = "tags";
    private static final String EXTENSIBLE = "extensible";
    private static final String REQUIRED = "required";
    /** The field of a change that holds an order, before and after. */
    private static final String ORDER = "order";
    /** What stands for the repository of a project that is in none. */
    private static final String LOCAL = "local";

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Announces a tag type created.
     *
     * @param tags the tags the tag type was created with, empty when it has none yet
     */
    public void typeCreated(TagType type, Collection<String> tags) {
        var details = new LinkedHashMap<String, Object>();
        details.put(EXTENSIBLE, type.isExtensible());
        details.put(REQUIRED, !type.isNullable());
        details.putAll(tagsOf(tags));
        announceType(AuditAction.TAG_TYPE_CREATED, type.getName(), details);
    }

    /**
     * Announces what a change did to a tag type: its name, and whether it is extensible and required. Nothing is
     * announced when the tag type kept every value.
     *
     * @param before     the tag type as it was, read before the change
     * @param name       the name it has now
     * @param nullable   whether a project may now have no value of it
     * @param extensible whether it now takes new values from any user
     */
    public void typeChanged(TagType before, String name, boolean nullable, boolean extensible) {
        var changes = new AuditChanges()
                .compare("name", before.getName(), name)
                .compare(EXTENSIBLE, before.isExtensible(), extensible)
                .compare(REQUIRED, !before.isNullable(), !nullable);
        announceChanged(AuditAction.TAG_TYPE_UPDATED, name, changes);
    }

    /**
     * Announces a tag type deleted.
     *
     * @param tags the tags deleted with it
     */
    public void typeDeleted(String type, Collection<String> tags) {
        announceType(AuditAction.TAG_TYPE_DELETED, type, tagsOf(tags));
    }

    /**
     * Announces the tag types arranged in another order. Nothing is announced when the order is the same.
     *
     * @param before the names of the tag types in the order they were listed in
     * @param after  the names in the order they are listed in now
     */
    public void typesReordered(List<String> before, List<String> after) {
        var changes = new AuditChanges().compare(ORDER, before, after);
        announceChanged(AuditAction.TAG_TYPES_REORDERED, String.join(", ", after), changes);
    }

    /** Announces a tag added to a tag type. */
    public void tagAdded(String type, String tag) {
        announceType(AuditAction.TAG_ADDED, type, Map.of(TAG, tag));
    }

    /** Announces a tag of a tag type renamed. Nothing is announced when the name is the same. */
    public void tagRenamed(String type, String before, String after) {
        announceChanged(AuditAction.TAG_RENAMED, type, new AuditChanges().compare(TAG, before, after));
    }

    /** Announces a tag deleted from a tag type. */
    public void tagDeleted(String type, String tag) {
        announceType(AuditAction.TAG_DELETED, type, Map.of(TAG, tag));
    }

    /**
     * Announces the project name templates changed: the templates added and removed. When the templates that
     * stayed changed places, which is their priority, the order before and after is announced too. Nothing is
     * announced when the templates are the same.
     *
     * @param before the templates as they were, the first one of the highest priority
     * @param after  the templates as they are now
     */
    public void templatesChanged(List<String> before, List<String> after) {
        var details = new LinkedHashMap<String, Object>(AuditChanges.addedAndRemoved(before, after));
        if (!keptOf(before, after).equals(keptOf(after, before))) {
            details.putAll(changedOf(new AuditChanges().compare(ORDER, before, after)));
        }
        if (!details.isEmpty()) {
            announce(AuditAction.TAG_TEMPLATES_CHANGED, AuditTargetType.TAG_TEMPLATES, null, details);
        }
    }

    /** The templates the other list holds too, in the order they stand in. */
    private static List<String> keptOf(List<String> templates, List<String> others) {
        return templates.stream().filter(others::contains).toList();
    }

    /**
     * Announces the projects a fill gave the tags the project name templates derive for them, all in one event.
     * Nothing is announced when the fill changed no project.
     *
     * <p>A project is named by its business name. Projects of one name are told apart by their repository and
     * folder, which follow the name; a project kept only in the workspace of the user is told apart as local.
     *
     * @param projects the tag values each changed project got, by tag type
     */
    public void filled(Map<RulesProject, Map<String, String>> projects) {
        if (projects.isEmpty()) {
            return;
        }
        var named = new LinkedHashMap<String, Map<String, String>>();
        try {
            projects.forEach((project, tags) -> {
                var name = nameOf(project, projects.keySet());
                // A project may be named as another one is told apart: no project gives way to another.
                while (named.containsKey(name)) {
                    name = "%s (%s:/%s)".formatted(name, repositoryOf(project), project.getRealPath());
                }
                named.put(name, tags);
            });
            var names = String.join(", ", named.keySet());
            announce(AuditAction.TAGS_FILLED, AuditTargetType.PROJECT, names, Map.of("projects", named));
        } catch (RuntimeException e) {
            log.error("Failed to record the tags filled for the projects {} in the audit log.", named.keySet(), e);
        }
    }

    /** The name of a filled project, with its repository and folder when another filled project has that name. */
    private static String nameOf(RulesProject project, Collection<RulesProject> filled) {
        var name = project.getBusinessName();
        if (filled.stream().filter(other -> name.equals(other.getBusinessName())).count() == 1) {
            return name;
        }
        var repository = repositoryOf(project);
        // A local project is in no design repository: it is told apart by its folder in the workspace.
        var path = project.isLocalOnly() ? project.getRealPath() : Place.pathOf(project);
        return path == null ? "%s (%s)".formatted(name, repository) : "%s (%s:/%s)".formatted(name, repository, path);
    }

    /** The design repository of a project, or {@value #LOCAL} for a project kept only in the workspace of the user. */
    private static String repositoryOf(RulesProject project) {
        return project.isLocalOnly() ? LOCAL : project.getDesignRepository().getId();
    }

    /** The tags of a tag type as a detail, which is left out when the tag type has none. */
    private static Map<String, List<String>> tagsOf(Collection<String> tags) {
        return tags.isEmpty() ? Map.of() : Map.of(TAGS, List.copyOf(tags));
    }

    private static Map<String, ?> changedOf(AuditChanges changes) {
        return Map.of(SettingsAuditor.CHANGED, changes.asMap());
    }

    /** Announces what changed in a tag type, and nothing when no value did. */
    private void announceChanged(AuditAction action, String type, AuditChanges changes) {
        if (!changes.isEmpty()) {
            announceType(action, type, changedOf(changes));
        }
    }

    private void announceType(AuditAction action, String type, Map<String, ?> details) {
        announce(action, AuditTargetType.TAG_TYPE, type, details);
    }

    private void announce(AuditAction action, AuditTargetType targetType, @Nullable String target,
                          Map<String, ?> details) {
        eventPublisher.publishEvent(AuditEvent.builder()
                .action(action)
                .targetType(targetType)
                .target(target)
                .details(details)
                .build());
    }
}
