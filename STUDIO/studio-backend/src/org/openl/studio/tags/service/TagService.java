package org.openl.studio.tags.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.dao.TagDao;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.studio.audit.service.TagAuditor;

/**
 * Keeps the tags of the tag types.
 *
 * <p>Every change is announced to the audit log in the transaction of the change: a tag added, renamed or deleted.
 * So is a tag created for an extensible tag type when a project brings a value the tag type does not have.
 */
@RequiredArgsConstructor
@Service
public class TagService {

    private final TagDao tagDao;
    private final TagAuditor tagAuditor;

    public List<Tag> getAll() {
        return tagDao.getAll();
    }

    /** Adds the tag to its tag type. */
    @Transactional
    public void save(Tag tag) {
        tagDao.save(tag);
        tagAuditor.tagAdded(tag.getType().getName(), tag.getName());
    }

    /**
     * Gives a tag another name.
     *
     * @return {@code false}, changing nothing, when the ID names no tag
     */
    @Transactional
    public boolean rename(Long id, String name) {
        var tag = tagDao.getById(id);
        if (tag == null) {
            return false;
        }
        var before = tag.getName();
        tag.setName(name);
        tagDao.update(tag);
        tagAuditor.tagRenamed(tag.getType().getName(), before, name);
        return true;
    }

    /**
     * Deletes a tag from its tag type.
     *
     * @return {@code false} when the ID names no tag
     */
    @Transactional
    public boolean delete(Long id) {
        var tag = tagDao.getById(id);
        if (tag == null || !tagDao.deleteById(id)) {
            return false;
        }
        tagAuditor.tagDeleted(tag.getType().getName(), tag.getName());
        return true;
    }

    public Tag getById(Long id) {
        return tagDao.getById(id);
    }

    public Tag getByName(Long tagTypeId, String name) {
        return tagDao.getByName(tagTypeId, name);
    }
}
