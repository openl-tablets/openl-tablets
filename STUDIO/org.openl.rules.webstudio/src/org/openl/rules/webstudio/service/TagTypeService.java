package org.openl.rules.webstudio.service;

import java.util.List;

import org.openl.rules.security.standalone.dao.TagTypeDao;
import org.openl.rules.security.standalone.persistence.TagType;

public class TagTypeService {

    private TagTypeDao tagTypeDao;

    public void setTagTypeDao(TagTypeDao tagTypeDao) {
        this.tagTypeDao = tagTypeDao;
    }

    /**
     * TODO: Should we replace TagType with non-hibernate-dependent class?
     * @return
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

    public void delete(String name) {
        tagTypeDao.deleteByName(name);
    }

    public TagType getByName(String name) {
        return tagTypeDao.getByName(name);
    }
}
