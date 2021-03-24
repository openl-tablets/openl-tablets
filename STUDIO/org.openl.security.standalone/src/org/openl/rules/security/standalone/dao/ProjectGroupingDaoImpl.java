package org.openl.rules.security.standalone.dao;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.openl.rules.security.standalone.persistence.ProjectGrouping;
import org.openl.rules.security.standalone.persistence.TagType;
import org.springframework.transaction.annotation.Transactional;

public class ProjectGroupingDaoImpl extends BaseHibernateDao<ProjectGrouping> implements ProjectGroupingDao {
    @Override
    @Transactional
    public ProjectGrouping getByLogin(final String login) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<ProjectGrouping> criteria = builder.createQuery(ProjectGrouping.class);
        Root<ProjectGrouping> u = criteria.from(ProjectGrouping.class);
        criteria.select(u).where(builder.equal(u.get("loginName"), login)).distinct(true);
        List<ProjectGrouping> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}
