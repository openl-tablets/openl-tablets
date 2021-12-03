package org.openl.rules.security.standalone.dao;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.hibernate.Session;
import org.openl.rules.security.standalone.persistence.ExternalGroup;
import org.openl.rules.security.standalone.persistence.Group;
import org.springframework.stereotype.Component;

/**
 * External Groups data access object implementation. This implementation doesn't care about transactions. All
 * transactions must be handled by service!
 *
 * @author Vladyslav Pikus
 */
@Component("externalGroupDao")
public class ExternalGroupDaoImpl extends BaseHibernateDao<ExternalGroup> implements ExternalGroupDao {

    public static final char ESCAPE_CHAR = '\\';

    @Override
    public void deleteAllForUser(String loginName) {
        getSession().createQuery("DELETE ExternalGroup ext where ext.loginName = :loginName")
            .setParameter("loginName", loginName)
            .executeUpdate();
    }

    @Override
    public List<ExternalGroup> findAllForUser(String loginName) {
        Session session = getSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ExternalGroup> query = cb.createQuery(ExternalGroup.class);
        Root<ExternalGroup> root = query.from(ExternalGroup.class);

        query.select(root).where(getPredicatesAllForUser(root, cb, loginName));

        return session.createQuery(query).getResultList();
    }

    @Override
    public long countAllForUser(String loginName) {
        Session session = getSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<ExternalGroup> root = query.from(ExternalGroup.class);

        query.select(cb.count(root)).where(getPredicatesAllForUser(root, cb, loginName));

        return session.createQuery(query).getSingleResult();
    }

    private Predicate[] getPredicatesAllForUser(Root<ExternalGroup> root, CriteriaBuilder cb, String loginName) {
        return new Predicate[] { cb.equal(root.get("loginName"), loginName) };
    }

    @Override
    public List<Group> findMatchedForUser(String loginName) {
        Session session = getSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Group> query = cb.createQuery(Group.class);
        Root<Group> groupRoot = query.from(Group.class);

        query.select(groupRoot).where(getPredicatesMatchedForUser(groupRoot, query, cb, loginName));

        return session.createQuery(query).getResultList();
    }

    @Override
    public long countMatchedForUser(String loginName) {
        Session session = getSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Group> groupRoot = query.from(Group.class);

        query.select(cb.count(groupRoot)).where(getPredicatesMatchedForUser(groupRoot, query, cb, loginName));

        return session.createQuery(query).getSingleResult();
    }

    private Predicate[] getPredicatesMatchedForUser(Root<Group> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            String loginName) {
        Root<ExternalGroup> extGroupRoot = query.from(ExternalGroup.class);

        return new Predicate[] { cb.equal(extGroupRoot.get("loginName"), loginName),
                cb.equal(root.get("name"), extGroupRoot.get("groupName")) };
    }

    @Override
    public List<ExternalGroup> findNotMatchedForUser(String loginName) {
        Session session = getSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ExternalGroup> query = cb.createQuery(ExternalGroup.class);
        Root<ExternalGroup> extGroupRoot = query.from(ExternalGroup.class);

        query.select(extGroupRoot).where(getPredicatesNotMatchedForUser(extGroupRoot, query, cb, loginName));

        return session.createQuery(query).getResultList();
    }

    @Override
    public long countNotMatchedForUser(String loginName) {
        Session session = getSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<ExternalGroup> extGroupRoot = query.from(ExternalGroup.class);

        query.select(cb.count(extGroupRoot)).where(getPredicatesNotMatchedForUser(extGroupRoot, query, cb, loginName));

        return session.createQuery(query).getSingleResult();
    }

    private Predicate[] getPredicatesNotMatchedForUser(Root<ExternalGroup> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            String loginName) {
        Subquery<String> sqGroup = query.subquery(String.class);
        Root<Group> rootGroup = sqGroup.from(Group.class);

        return new Predicate[] { cb.equal(root.get("loginName"), loginName),
                root.get("groupName").in(sqGroup.select(rootGroup.get("name"))).not() };
    }

    @Override
    public List<String> findAllByName(String groupName, int limit) {
        Session session = getSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<ExternalGroup> extGroupRoot = query.from(ExternalGroup.class);

        query.select(extGroupRoot.get("groupName"))
            .distinct(true)
            .where(cb
                .like(cb.lower(extGroupRoot.get("groupName")), "%" + escape(groupName) + "%", cb.literal(ESCAPE_CHAR)));
        return session.createQuery(query).setMaxResults(limit).getResultList();
    }

    private static String escape(String searchTerm) {
        searchTerm = searchTerm.toLowerCase();
        searchTerm = searchTerm.replace("\\", ESCAPE_CHAR + "\\");
        searchTerm = searchTerm.replace("[", ESCAPE_CHAR + "[");
        searchTerm = searchTerm.replace("_", ESCAPE_CHAR + "_");
        return searchTerm.replace("%", ESCAPE_CHAR + "%");
    }
}
