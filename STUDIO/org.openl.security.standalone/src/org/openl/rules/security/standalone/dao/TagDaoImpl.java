package org.openl.rules.security.standalone.dao;

import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.persistence.Tag;

public class TagDaoImpl extends BaseHibernateDao<Tag> implements TagDao {
    @Override
    @Transactional
    public Tag getById(final Long id) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<Tag> criteria = builder.createQuery(Tag.class);
        Root<Tag> u = criteria.from(Tag.class);
        criteria.select(u).where(builder.equal(u.get("id"), id)).distinct(true);
        List<Tag> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    @Transactional
    public Tag getByName(Long tagTypeId, String name) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<Tag> criteria = builder.createQuery(Tag.class);
        Root<Tag> u = criteria.from(Tag.class);
        // Case insensitive
        criteria.select(u)
                .where(builder.and(builder.equal(u.get("type").get("id"), tagTypeId),
                        builder.equal(builder.lower(u.get("name")), name.toLowerCase())))
                .distinct(true);
        List<Tag> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    @Transactional
    public List<Tag> getAll() {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<Tag> criteria = builder.createQuery(Tag.class);
        Root<Tag> root = criteria.from(Tag.class);
        criteria.select(root)
                .orderBy(builder.asc(builder.upper(root.get("type").get("name"))),
                        builder.asc(builder.upper(root.get("name"))));
        return getSession().createQuery(criteria).getResultList();
    }

    @Override
    @Transactional
    public List<Tag> getByTagType(String tagType) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<Tag> criteria = builder.createQuery(Tag.class);
        Root<Tag> root = criteria.from(Tag.class);
        criteria.select(root)
                .where(builder.and(builder.equal(root.get("type").get("name"), tagType)))
                .orderBy(builder.asc(builder.upper(root.get("name"))));
        return getSession().createQuery(criteria).getResultList();
    }

    @Override
    @Transactional
    public Tag getByTagTypeAndName(String tagType, String tagName) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<Tag> criteria = builder.createQuery(Tag.class);
        Root<Tag> root = criteria.from(Tag.class);
        criteria.select(root)
                .where(builder.and(builder.equal(builder.lower(root.get("type").get("name")), tagType.toLowerCase()),
                        builder.equal(builder.lower(root.get("name")), tagName.toLowerCase())));
        final List<Tag> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Transactional
    @Override
    public boolean deleteById(Long id) {
        return getSession().createNativeQuery("delete from OpenL_Tags where id = :id")
                .setParameter("id", id)
                .executeUpdate() > 0;
    }
}
