package org.openl.studio.tags.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.dao.TagDao;
import org.openl.rules.security.standalone.dao.TagTypeDao;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.studio.tags.model.TagDTO;
import org.openl.studio.tags.model.TagTypeDTO;

@RequiredArgsConstructor
@Service
public class TagTypeService {

    private final TagTypeDao tagTypeDao;
    private final TagDao tagDao;

    public List<TagType> getAllTagTypes() {
        return tagTypeDao.getAll();
    }

    public void save(TagType tagType) {
        tagTypeDao.save(tagType);
    }

    public void update(TagType tagType) {
        tagTypeDao.update(tagType);
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
        // The types are read in this transaction, so the changed ones are written when it commits.
        for (var type : types) {
            var position = ids.indexOf(type.getId());
            type.setDisplayOrder(position < 0 ? null : position);
        }
        return true;
    }

    public boolean delete(Long id) {
        return tagTypeDao.deleteById(id);
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
