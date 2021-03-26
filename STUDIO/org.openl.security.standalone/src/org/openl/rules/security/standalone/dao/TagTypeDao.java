package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.openl.rules.security.standalone.persistence.TagType;
import org.springframework.transaction.annotation.Transactional;

public interface TagTypeDao extends Dao<TagType> {
    @Transactional
    TagType getById(Long id);

    @Transactional
    TagType getByName(String name);

    @Transactional
    List<TagType> getAll();

    @Transactional
    void deleteByName(String name);
}
