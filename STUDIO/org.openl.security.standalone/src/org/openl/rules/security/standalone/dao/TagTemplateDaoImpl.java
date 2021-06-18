package org.openl.rules.security.standalone.dao;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.openl.rules.security.standalone.persistence.TagTemplate;
import org.springframework.transaction.annotation.Transactional;

public class TagTemplateDaoImpl extends BaseHibernateDao<TagTemplate> implements TagTemplateDao {

    @Override
    @Transactional
    public TagTemplate getByTemplate(final String template) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<TagTemplate> criteria = builder.createQuery(TagTemplate.class);
        Root<TagTemplate> u = criteria.from(TagTemplate.class);
        criteria.select(u).where(builder.equal(u.get("template"), template)).distinct(true);
        List<TagTemplate> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    @Transactional
    public List<TagTemplate> getAll() {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<TagTemplate> criteria = builder.createQuery(TagTemplate.class);
        Root<TagTemplate> root = criteria.from(TagTemplate.class);
        criteria.select(root).orderBy(builder.asc(root.get("priority")));
        return getSession().createQuery(criteria).getResultList();
    }

    @Transactional
    @Override
    public void deleteAll() {
        getSession().createNativeQuery("delete from OpenL_Tag_Templates").executeUpdate();
    }
}
