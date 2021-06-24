package org.openl.rules.webstudio.service;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.rest.tags.TagDTO;
import org.openl.rules.rest.tags.TagTypeDTO;
import org.openl.rules.security.standalone.dao.TagDao;
import org.openl.rules.security.standalone.dao.TagTypeDao;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.springframework.transaction.annotation.Transactional;

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

    public void delete(String name) {
        tagTypeDao.deleteByName(name);
    }

    public TagType getById(Long id) {
        return tagTypeDao.getById(id);
    }

    public TagType getByName(String name) {
        return tagTypeDao.getByName(name);
    }

    @Transactional
    public List<TagTypeDTO> getAll() {
        List<TagTypeDTO> result = new ArrayList<>();

        final List<TagType> all = tagTypeDao.getAll();
        for (TagType tagType : all) {
            TagTypeDTO typeDTO = new TagTypeDTO();
            typeDTO.setId(tagType.getId());
            typeDTO.setName(tagType.getName());
            typeDTO.setNullable(tagType.isNullable());
            typeDTO.setExtensible(tagType.isExtensible());

            final ArrayList<TagDTO> tagDTOs = new ArrayList<>();
            final List<Tag> tags = tagDao.getByTagType(tagType.getName());
            for (Tag tag : tags) {
                TagDTO tagDTO = new TagDTO();
                tagDTO.setId(tag.getId());
                tagDTO.setName(tag.getName());
                tagDTO.setTagTypeId(tag.getType().getId());
                tagDTOs.add(tagDTO);
            }
            typeDTO.setTags(tagDTOs);

            result.add(typeDTO);
        }

        return result;
    }
}
