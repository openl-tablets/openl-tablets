package org.openl.rules.security.standalone.dao;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.openl.rules.security.standalone.persistence.OpenLProject;
import org.openl.rules.security.standalone.persistence.Tag;
import org.springframework.transaction.annotation.Transactional;

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
        criteria.select(root).where(builder.and(builder.equal(root.get("repositoryId"), repoId),
            builder.equal(root.get("projectPath"), projectPath)));
        List<OpenLProject> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    @Transactional
    public List<Tag> getTagsForProject(String repoId, String projectPath) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<Tag> criteria = builder.createQuery(Tag.class);
        final Root<OpenLProject> root = criteria.from(OpenLProject.class);
        Join<OpenLProject, Tag> tags = root.join("tags");
        criteria.select(tags)
            .where(builder.and(builder.equal(root.get("repositoryId"), repoId),
                builder.equal(root.get("projectPath"), projectPath)))
            .orderBy(builder.asc(builder.upper(tags.get("tagType"))), builder.asc(builder.upper(tags.get("name"))));
        return getSession().createQuery(criteria).getResultList();
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
}
