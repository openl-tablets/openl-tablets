package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.persistence.TagTemplate;

public interface TagTemplateDao extends Dao<TagTemplate> {
    @Transactional
    TagTemplate getByTemplate(String template);

    @Transactional
    List<TagTemplate> getAll();

    /**
     * The texts of the templates, the one of the highest priority first.
     *
     * <p>Only the texts are read, so the templates can be replaced in the same transaction.
     */
    @Transactional
    List<String> getTemplates();

    @Transactional
    void deleteAll();
}
