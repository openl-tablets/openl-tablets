package org.openl.studio.tags.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;

/**
 * The configured tag types and their values, as one immutable snapshot.
 *
 * <p>The catalog does not govern what a project is tagged with: the {@code tags.properties} file of the
 * project is the source of truth, and whatever it names is shown and kept, configured or not. The catalog
 * is what an administrator offers on top of that — the suggested types and values of the editor, and what
 * the server-side flows (filling tags from templates, registering the tags of a created project) may
 * assign.
 *
 * <p>Types and values are matched ignoring case, the way they are looked up in the database, and the
 * configured spelling is what the catalog reports back.
 */
public final class TagCatalog {


    private final Map<String, TypeEntry> typesByName;

    private TagCatalog(Map<String, TypeEntry> typesByName) {
        this.typesByName = typesByName;
    }

    /** A configured tag type together with the values it allows. */
    private record TypeEntry(TagType type, Map<String, String> valuesByName) {
    }

    /**
     * Builds the catalog from the configured types and every value of theirs.
     *
     * @param types configured tag types
     * @param tags  configured tag values of those types
     */
    public static TagCatalog of(List<TagType> types, List<Tag> tags) {
        var byName = new TreeMap<String, TypeEntry>(String.CASE_INSENSITIVE_ORDER);
        types.forEach(type -> byName.put(type.getName(),
                new TypeEntry(type, new TreeMap<>(String.CASE_INSENSITIVE_ORDER))));
        tags.forEach(tag -> {
            var entry = byName.get(tag.getType().getName());
            if (entry != null) {
                entry.valuesByName().put(tag.getName(), tag.getName());
            }
        });
        return new TagCatalog(byName);
    }

    /** The configured type of that name. */
    public Optional<TagType> type(String typeName) {
        return Optional.ofNullable(typesByName.get(typeName)).map(TypeEntry::type);
    }

    /**
     * The configured value of a type, as an administrator spelled it.
     *
     * @return the configured value, or empty when the type or the value is not configured
     */
    public Optional<String> configuredValue(String typeName, String value) {
        return Optional.ofNullable(typesByName.get(typeName))
                .map(entry -> entry.valuesByName().get(value));
    }

}
