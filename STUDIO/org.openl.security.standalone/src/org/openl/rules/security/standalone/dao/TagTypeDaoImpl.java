package org.openl.rules.security.standalone.dao;

import java.util.List;
import java.util.Locale;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.persistence.TagType;

public class TagTypeDaoImpl extends BaseHibernateDao<TagType> implements TagTypeDao {
    @Override
    public TagType getById(Long id) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<TagType> criteria = builder.createQuery(TagType.class);
        Root<TagType> u = criteria.from(TagType.class);
        criteria.select(u).where(builder.equal(u.get("id"), id)).distinct(true);
        List<TagType> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.getFirst();
    }

    @Override
    @Transactional
    public TagType getByName(final String name) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<TagType> criteria = builder.createQuery(TagType.class);
        Root<TagType> u = criteria.from(TagType.class);
        // Case insensitive
        criteria.select(u).where(builder.equal(builder.lower(u.get("name")), name.toLowerCase(Locale.ROOT))).distinct(true);
        List<TagType> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.getFirst();
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

    @Override
    public boolean deleteById(Long id) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var delete = cb.createCriteriaDelete(TagType.class);
        var root = delete.from(TagType.class);
        delete.where(cb.equal(root.get("id"), id));
        return session.createMutationQuery(delete).executeUpdate() > 0;
    }

    @Transactional
    @Override
    public void deleteByName(String name) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var delete = cb.createCriteriaDelete(TagType.class);
        var root = delete.from(TagType.class);
        delete.where(cb.equal(root.get("name"), name));
        session.createMutationQuery(delete).executeUpdate();
    }
}
