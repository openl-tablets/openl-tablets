package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.springframework.transaction.annotation.Transactional;

public interface TagDao extends Dao<Tag> {
    @Transactional
    Tag getById(Long id);

    @Transactional
    Tag getByName(String name);

    @Transactional
    List<Tag> getAll();

    @Transactional
    List<Tag> getByTagType(String tagType);

    @Transactional
    Tag getByTagTypeAndName(String tagType, String tagName);

    @Transactional
    void deleteById(Long id);
}
