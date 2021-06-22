package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.openl.rules.security.standalone.persistence.TagTemplate;
import org.openl.rules.security.standalone.persistence.TagType;
import org.springframework.transaction.annotation.Transactional;

public interface TagTemplateDao extends Dao<TagTemplate> {
    @Transactional
    TagTemplate getByTemplate(String template);

    @Transactional
    List<TagTemplate> getAll();

    @Transactional
    void deleteAll();
}
