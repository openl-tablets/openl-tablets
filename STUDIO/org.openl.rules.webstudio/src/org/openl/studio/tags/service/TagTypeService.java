package org.openl.studio.tags.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.dao.TagDao;
import org.openl.rules.security.standalone.dao.TagTypeDao;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.studio.tags.model.TagDTO;
import org.openl.studio.tags.model.TagTypeDTO;

@Service
public class TagTypeService {

    private final TagTypeDao tagTypeDao;
    private final TagDao tagDao;

    public TagTypeService(TagTypeDao tagTypeDao, TagDao tagDao) {
        this.tagTypeDao = tagTypeDao;
        this.tagDao = tagDao;
    }

    /**
     * TODO: Should we replace TagType with non-hibernate-dependent class?
     */
    public List<TagType> getAllTagTypes() {
        return tagTypeDao.getAll();
    }

    public void save(TagType tagType) {
        tagTypeDao.save(tagType);
    }

    public void update(TagType tagType) {
        tagTypeDao.update(tagType);
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
