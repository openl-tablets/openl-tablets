package org.openl.rules.webstudio.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openl.rules.security.standalone.dao.TagDao;
import org.openl.rules.security.standalone.persistence.Tag;

public class TagService {

    private final TagDao tagDao;

    public TagService(TagDao tagDao) {
        this.tagDao = tagDao;
    }

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

    public boolean delete(Long id) {
        return tagDao.deleteById(id);
    }

    public Tag getById(Long id) {
        return tagDao.getById(id);
    }

    public Tag getByName(Long tagTypeId, String name) {
        return tagDao.getByName(tagTypeId, name);
    }

    public Tag getByTypeNameAndName(String key, String value) {
        return tagDao.getByTagTypeAndName(key, value);
    }
}
