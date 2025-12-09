package org.openl.studio.tags.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.dao.TagDao;
import org.openl.rules.security.standalone.persistence.Tag;

@Service
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

    @Transactional
    public void save(Tag tag) {
        tagDao.save(tag);
    }

    @Transactional
    public void update(Tag tag) {
        tagDao.update(tag);
    }

    @Transactional
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
