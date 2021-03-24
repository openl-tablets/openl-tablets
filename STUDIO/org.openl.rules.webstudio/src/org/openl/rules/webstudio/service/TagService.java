package org.openl.rules.webstudio.service;

import java.util.List;

import org.openl.rules.security.standalone.dao.TagDao;
import org.openl.rules.security.standalone.persistence.Tag;

public class TagService {

    private TagDao tagDao;

    public List<Tag> getAll() {
        return tagDao.getAll();
    }

    public List<Tag> getByTagType(String type) {
        return tagDao.getByTagType(type);
    }

    public void save(Tag tag) {
        tagDao.save(tag);
    }

    public void update(Tag tag) {
        tagDao.update(tag);
    }

    public void delete(Long id) {
        tagDao.deleteById(id);
    }

    public Tag getById(Long id) {
        return tagDao.getById(id);
    }

    public void setTagDao(TagDao tagDao) {
        this.tagDao = tagDao;
    }
}
