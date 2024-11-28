package org.openl.rules.security.standalone.dao;

import java.util.List;
import java.util.Map;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.persistence.OpenLProject;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;

public class OpenLProjectDaoImpl extends BaseHibernateDao<OpenLProject> implements OpenLProjectDao {
    @Override
    @Transactional
    public OpenLProject getById(final Long id) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<OpenLProject> criteria = builder.createQuery(OpenLProject.class);
        Root<OpenLProject> u = criteria.from(OpenLProject.class);
        criteria.select(u).where(builder.equal(u.get("id"), id)).distinct(true);
        List<OpenLProject> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    @Transactional
    public OpenLProject getProject(String repoId, String projectPath) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<OpenLProject> criteria = builder.createQuery(OpenLProject.class);
        Root<OpenLProject> root = criteria.from(OpenLProject.class);
        criteria.select(root).where(getSelectProjectPredicate(root, builder, repoId, projectPath));
        List<OpenLProject> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<Tag> getTagsForProject(String repoId, String projectPath) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<Tag> criteria = builder.createQuery(Tag.class);
        final Root<OpenLProject> root = criteria.from(OpenLProject.class);

        Join<OpenLProject, Tag> tags = root.join("tags");
        criteria.select(tags)
                .where(getSelectProjectPredicate(root, builder, repoId, projectPath));
        return getSession().createQuery(criteria).getResultList();
    }

    private Predicate[] getSelectProjectPredicate(Root<OpenLProject> root,
                                                  CriteriaBuilder cb,
                                                  String repoId,
                                                  String projectPath) {
        return new Predicate[]{cb.equal(root.get("repositoryId"), repoId),
                cb.equal(root.get("projectPath"), projectPath)};
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        getSession().createNativeQuery("delete from OpenL_Projects where id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    @Transactional
    public List<OpenLProject> getProjectsForTag(Long id) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<OpenLProject> criteria = builder.createQuery(OpenLProject.class);
        Root<OpenLProject> root = criteria.from(OpenLProject.class);
        Join<OpenLProject, Tag> tagsField = root.join("tags");
        criteria.select(root).where(builder.equal(tagsField.get("id"), id));
        return getSession().createQuery(criteria).getResultList();
    }

    @Override
    public boolean isProjectHasTags(String repoId, String projectPath, Map<String, String> tags) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(OpenLProject.class);

        var tagsJoin = root.<OpenLProject, Tag>join("tags");
        var tagTypeJoin = tagsJoin.<Tag, TagType>join("type");

        Predicate[] tagPredicates = new Predicate[tags.size()];
        int i = 0;
        for (var entry : tags.entrySet()) {
            tagPredicates[i++] = cb.and(cb.equal(tagTypeJoin.get("name"), entry.getKey()),
                    cb.equal(tagsJoin.get("name"), entry.getValue()));
        }

        query.select(cb.countDistinct(tagsJoin))
                .where(cb.and(getSelectProjectPredicate(root, cb, repoId, projectPath)), cb.or(tagPredicates));

        return session.createQuery(query).getSingleResult().equals((long) tags.size());
    }
}
