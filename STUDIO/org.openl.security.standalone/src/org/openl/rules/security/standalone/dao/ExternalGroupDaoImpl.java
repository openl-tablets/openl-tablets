package org.openl.rules.security.standalone.dao;

import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.stereotype.Component;

import org.openl.rules.security.standalone.persistence.ExternalGroup;
import org.openl.rules.security.standalone.persistence.Group;

/**
 * External Groups data access object implementation. This implementation doesn't care about transactions. All
 * transactions must be handled by service!
 *
 * @author Vladyslav Pikus
 */
@Component("externalGroupDao")
public class ExternalGroupDaoImpl extends BaseHibernateDao<ExternalGroup> implements ExternalGroupDao {

    private static final String LOGIN_NAME = "loginName";
    private static final String GROUP_NAME = "groupName";

    @Override
    public void deleteAll() {
        getSession().createMutationQuery("DELETE ExternalGroup").executeUpdate();
    }

    @Override
    public void deleteAllForUser(String loginName) {
        getSession().createMutationQuery("DELETE ExternalGroup ext where ext.loginName = :loginName")
                .setParameter(LOGIN_NAME, loginName)
                .executeUpdate();
    }

    @Override
    public List<ExternalGroup> findAllForUser(String loginName) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var query = cb.createQuery(ExternalGroup.class);
        var root = query.from(ExternalGroup.class);

        query.select(root).where(getPredicatesAllForUser(root, cb, loginName));

        return session.createQuery(query).getResultList();
    }

    @Override
    public long countAllForUser(String loginName) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(ExternalGroup.class);

        query.select(cb.count(root)).where(getPredicatesAllForUser(root, cb, loginName));

        return session.createQuery(query).getSingleResult();
    }

    private Predicate[] getPredicatesAllForUser(Root<ExternalGroup> root, CriteriaBuilder cb, String loginName) {
        return new Predicate[]{cb.equal(root.get(LOGIN_NAME), loginName)};
    }

    @Override
    public List<Group> findMatchedForUser(String loginName) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var query = cb.createQuery(Group.class);
        var groupRoot = query.from(Group.class);

        query.select(groupRoot).where(getPredicatesMatchedForUser(groupRoot, query, cb, loginName));

        return session.createQuery(query).getResultList();
    }

    @Override
    public long countMatchedForUser(String loginName) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var groupRoot = query.from(Group.class);

        query.select(cb.count(groupRoot)).where(getPredicatesMatchedForUser(groupRoot, query, cb, loginName));

        return session.createQuery(query).getSingleResult();
    }

    private Predicate[] getPredicatesMatchedForUser(Root<Group> root,
                                                    CriteriaQuery<?> query,
                                                    CriteriaBuilder cb,
                                                    String loginName) {
        var extGroupRoot = query.from(ExternalGroup.class);

        return new Predicate[]{cb.equal(extGroupRoot.get(LOGIN_NAME), loginName),
                cb.equal(root.get("name"), extGroupRoot.get(GROUP_NAME))};
    }

    @Override
    public List<ExternalGroup> findNotMatchedForUser(String loginName) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var query = cb.createQuery(ExternalGroup.class);
        var extGroupRoot = query.from(ExternalGroup.class);

        query.select(extGroupRoot).where(getPredicatesNotMatchedForUser(extGroupRoot, query, cb, loginName));

        return session.createQuery(query).getResultList();
    }

    @Override
    public long countNotMatchedForUser(String loginName) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var extGroupRoot = query.from(ExternalGroup.class);

        query.select(cb.count(extGroupRoot)).where(getPredicatesNotMatchedForUser(extGroupRoot, query, cb, loginName));

        return session.createQuery(query).getSingleResult();
    }

    private Predicate[] getPredicatesNotMatchedForUser(Root<ExternalGroup> root,
                                                       CriteriaQuery<?> query,
                                                       CriteriaBuilder cb,
                                                       String loginName) {
        var sqGroup = query.subquery(String.class);
        var rootGroup = sqGroup.from(Group.class);

        return new Predicate[]{cb.equal(root.get(LOGIN_NAME), loginName),
                root.get(GROUP_NAME).in(sqGroup.select(rootGroup.get("name"))).not()};
    }

    @Override
    public List<String> findAllByName(String groupName, int limit) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var query = cb.createQuery(String.class);
        var extGroupRoot = query.from(ExternalGroup.class);

        query.select(extGroupRoot.get(GROUP_NAME))
                .distinct(true)
                .where(cb
                        .like(cb.lower(extGroupRoot.get(GROUP_NAME)), "%" + escape(groupName) + "%", cb.literal(ESCAPE_CHAR)));
        return session.createQuery(query).setMaxResults(limit).getResultList();
    }


    @Override
    public long countUsersInGroup(String groupName) {
        var cb = getSession().getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var extGroupRoot = query.from(ExternalGroup.class);

        query.select(cb.count(extGroupRoot))
                .where(cb.equal(extGroupRoot.get(GROUP_NAME), groupName));

        return getSession().createQuery(query).getSingleResult();
    }
}
