package org.openl.studio.tags.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.util.StringUtils;

/**
 * Matches tag assignments against the configured tag types for the flows that write tags on the server:
 * filling tags from the project name templates, and registering the tags a created or imported project
 * brought with it.
 *
 * <p>The {@code tags.properties} file of a project is the source of truth for what the project carries;
 * nothing here validates or filters it. The catalog only decides what these server-side flows may assign,
 * and a value of an extensible tag type that is new is created so it becomes selectable afterwards.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TagAssignmentValidator {

    private final TagCatalogProvider tagCatalogProvider;
    private final TagService tagService;

    /**
     * Keeps only the assignments that can be applied as is, creating the new values of extensible tag
     * types. An unknown tag type, and a value a tag type that is not extensible does not have, are
     * dropped instead of rejected.
     *
     * <p>This is what filling tags from project name templates needs: a template covers many projects at
     * once, and one value it cannot assign must not stop the rest.
     *
     * @param requested tag type name to value assignments
     * @return the assignments to persist, with the configured spelling of the type and of the value
     */
    public Map<String, String> applicable(Map<String, String> requested) {
        var catalog = tagCatalogProvider.get();
        var applicable = new LinkedHashMap<String, String>();
        requested.forEach((typeName, rawValue) -> {
            var value = StringUtils.trimToNull(rawValue);
            var type = catalog.type(typeName).orElse(null);
            if (value == null || type == null) {
                return;
            }
            assignable(catalog, type, value).ifPresent(assigned -> applicable.put(type.getName(), assigned));
        });
        return applicable;
    }

    /**
     * The value to store, or empty when the tag type is not extensible and does not have that value.
     *
     * <p>A new value of an extensible tag type is created here, so that a project that names it is no
     * different from a project that picked a configured one. A new value that is not a valid name is
     * dropped like any other value that cannot be assigned: these flows run after the project is
     * already saved, so one bad value must not fail the whole request.
     */
    private Optional<String> assignable(TagCatalog catalog, TagType type, String value) {
        var configured = catalog.configuredValue(type.getName(), value);
        if (configured.isPresent() || !type.isExtensible()) {
            return configured;
        }
        if (!NameChecker.checkName(value)) {
            log.warn("Tag value '{}' of type '{}' is not a valid name and was not registered.", value, type.getName());
            return Optional.empty();
        }
        register(type, value);
        return Optional.of(value);
    }

    private void register(TagType type, String value) {
        var tag = new Tag();
        tag.setType(type);
        tag.setName(value);
        tagService.save(tag);
        // The value is configured from now on, so whatever reads the catalog later in this request sees it.
        tagCatalogProvider.invalidate();
    }
}
