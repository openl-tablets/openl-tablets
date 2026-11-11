package org.openl.studio.tags.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.dao.TagDao;
import org.openl.rules.security.standalone.dao.TagTypeDao;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.studio.audit.service.TagAuditor;
import org.openl.studio.tags.model.TagDTO;
import org.openl.studio.tags.model.TagTypeDTO;

/**
 * Keeps the tag types of the tags configuration, with their order and their tags.
 *
 * <p>Every change is announced to the audit log in the transaction of the change: a tag type created, changed,
 * deleted or moved, and a tag added to it. A change that leaves every value as it was announces nothing.
 */
@RequiredArgsConstructor
@Service
public class TagTypeService {

    private final TagTypeDao tagTypeDao;
    private final TagDao tagDao;
    private final TagAuditor tagAuditor;

    public List<TagType> getAllTagTypes() {
        return tagTypeDao.getAll();
    }

    /** Creates the tag type, with no tags yet. */
    @Transactional
    public void save(TagType tagType) {
        tagTypeDao.save(tagType);
        tagAuditor.typeCreated(tagType, List.of());
    }

    /**
     * Gives a tag type another name, and sets whether it is extensible and whether a project may have no value
     * of it.
     *
     * @param id the ID of the tag type
     * @return {@code false}, changing nothing, when the ID names no tag type
     */
    @Transactional
    public boolean update(Long id, String name, boolean nullable, boolean extensible) {
        var type = tagTypeDao.getById(id);
        if (type == null) {
            return false;
        }
        // The event compares the tag type as it was, so it is announced before the values are set.
        tagAuditor.typeChanged(type, name, nullable, extensible);
        type.setName(name);
        type.setNullable(nullable);
        type.setExtensible(extensible);
        tagTypeDao.update(type);
        return true;
    }

    /**
     * Arranges the tag types in the given order. The tag types not listed follow the listed ones, by name.
     *
     * @param ids the IDs of the tag types, in the order to list them
     * @return {@code false}, changing nothing, when an ID names no tag type
     */
    @Transactional
    public boolean reorder(List<Long> ids) {
        var types = tagTypeDao.getAll();
        var known = types.stream().map(TagType::getId).collect(Collectors.toSet());
        if (!known.containsAll(ids)) {
            return false;
        }
        var before = namesOf(types);
        // The types are read in this transaction, so the changed ones are written when it commits.
        for (var type : types) {
            var position = ids.indexOf(type.getId());
            type.setDisplayOrder(position < 0 ? null : position);
        }
        // Listing them again writes the new positions first, so the names come in the order just set.
        tagAuditor.typesReordered(before, namesOf(tagTypeDao.getAll()));
        return true;
    }

    /**
     * Defines the given tags in the configuration, so that the projects carrying them are tagged as configured.
     *
     * <p>A tag type that does not exist yet is created as optional and not extensible, so that no project is
     * warned about it. A value its tag type does not have yet is added to it. Names match regardless of case,
     * so a tag type or a value that exists under another case is kept as it is.
     *
     * @param tags tag type names, each with the values to define for it; the names must be valid
     */
    @Transactional
    public void define(Map<String, List<String>> tags) {
        tags.forEach((typeName, values) -> {
            var type = tagTypeDao.getByName(typeName);
            if (type == null) {
                defineType(typeName, values);
            } else {
                defineValues(type, values).forEach(value -> tagAuditor.tagAdded(type.getName(), value));
            }
        });
    }

    /** Creates an optional tag type that is not extensible, with the given values. */
    private void defineType(String name, List<String> values) {
        var type = new TagType();
        type.setName(name);
        type.setNullable(true);
        tagTypeDao.save(type);
        tagAuditor.typeCreated(type, defineValues(type, values));
    }

    /** Adds the values the tag type does not have yet, and answers the ones it added. */
    private List<String> defineValues(TagType type, List<String> values) {
        var added = new ArrayList<String>();
        for (var value : values) {
            if (tagDao.getByName(type.getId(), value) == null) {
                var tag = new Tag();
                tag.setType(type);
                tag.setName(value);
                tagDao.save(tag);
                added.add(value);
            }
        }
        return added;
    }

    /**
     * Deletes a tag type with its tags.
     *
     * @return {@code false} when the ID names no tag type
     */
    @Transactional
    public boolean delete(Long id) {
        var type = tagTypeDao.getById(id);
        if (type == null) {
            return false;
        }
        var tags = tagDao.getNames(id);
        if (!tagTypeDao.deleteById(id)) {
            return false;
        }
        tagAuditor.typeDeleted(type.getName(), tags);
        return true;
    }

    private static List<String> namesOf(List<TagType> types) {
        return types.stream().map(TagType::getName).toList();
    }

    public TagType getById(Long id) {
        return tagTypeDao.getById(id);
    }

    public TagType getByName(String name) {
        return tagTypeDao.getByName(name);
    }

    @Transactional
    public List<TagTypeDTO> getAll() {
        // Every value is read in one query and grouped here: asking the database once per tag type turns
        // listing the configuration into a query per type.
        var tagsByType = tagDao.getAll().stream().collect(Collectors.groupingBy(tag -> tag.getType().getId()));
        return tagTypeDao.getAll().stream().map(tagType -> {
            var typeDTO = new TagTypeDTO();
            typeDTO.setId(tagType.getId());
            typeDTO.setName(tagType.getName());
            typeDTO.setNullable(tagType.isNullable());
            typeDTO.setExtensible(tagType.isExtensible());
            typeDTO.setTags(tagsByType.getOrDefault(tagType.getId(), List.of()).stream()
                    .map(TagTypeService::mapTag)
                    .collect(Collectors.toCollection(ArrayList::new)));
            return typeDTO;
        }).toList();
    }

    private static TagDTO mapTag(Tag tag) {
        var tagDTO = new TagDTO();
        tagDTO.setId(tag.getId());
        tagDTO.setName(tag.getName());
        tagDTO.setTagTypeId(tag.getType().getId());
        return tagDTO;
    }
}
