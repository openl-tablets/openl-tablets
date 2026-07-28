package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.persistence.TagTemplate;

public class TagTemplateDaoImpl extends BaseHibernateDao<TagTemplate> implements TagTemplateDao {

    @Override
    @Transactional
    public TagTemplate getByTemplate(final String template) {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(TagTemplate.class);
        var u = criteria.from(TagTemplate.class);
        criteria.select(u).where(builder.equal(u.get("template"), template)).distinct(true);
        List<TagTemplate> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.getFirst();
    }

    @Override
    @Transactional
    public List<TagTemplate> getAll() {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(TagTemplate.class);
        var root = criteria.from(TagTemplate.class);
        criteria.select(root).orderBy(builder.asc(root.get("priority")));
        return getSession().createQuery(criteria).getResultList();
    }

    @Transactional
    @Override
    public void deleteAll() {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var delete = cb.createCriteriaDelete(TagTemplate.class);
        delete.from(TagTemplate.class);
        session.createMutationQuery(delete).executeUpdate();
    }
}
