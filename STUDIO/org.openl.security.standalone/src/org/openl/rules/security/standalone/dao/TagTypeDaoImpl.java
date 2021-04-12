package org.openl.rules.security.standalone.dao;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.openl.rules.security.standalone.persistence.TagType;
import org.springframework.transaction.annotation.Transactional;

public class TagTypeDaoImpl extends BaseHibernateDao<TagType> implements TagTypeDao {
    @Override
    public TagType getById(Long id) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<TagType> criteria = builder.createQuery(TagType.class);
        Root<TagType> u = criteria.from(TagType.class);
        criteria.select(u).where(builder.equal(u.get("id"), id)).distinct(true);
        List<TagType> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    @Transactional
    public TagType getByName(final String name) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<TagType> criteria = builder.createQuery(TagType.class);
        Root<TagType> u = criteria.from(TagType.class);
        // Case insensitive
        criteria.select(u).where(builder.equal(builder.lower(u.get("name")), name.toLowerCase())).distinct(true);
        List<TagType> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    @Transactional
    public List<TagType> getAll() {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<TagType> criteria = builder.createQuery(TagType.class);
        Root<TagType> root = criteria.from(TagType.class);
        criteria.select(root).orderBy(builder.asc(builder.upper(root.get("name"))));
        return getSession().createQuery(criteria).getResultList();
    }

    @Transactional
    @Override
    public void deleteByName(String name) {
        getSession().createNativeQuery("delete from OpenL_Tag_Types where name = :name")
            .setParameter("name", name)
            .executeUpdate();
    }
}
