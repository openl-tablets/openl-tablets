package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import org.openl.rules.security.standalone.persistence.PersonalAccessToken;

/**
 * Hibernate implementation of {@link PersonalAccessTokenDao}.
 */
@Component("personalAccessTokenDao")
public class PersonalAccessTokenDaoImpl extends BaseHibernateDao<PersonalAccessToken> implements PersonalAccessTokenDao {

    private static final String PUBLIC_ID = "publicId";
    private static final String LOGIN_NAME = "loginName";

    @Override
    public PersonalAccessToken getByPublicId(String publicId) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var query = cb.createQuery(PersonalAccessToken.class);
        var root = query.from(PersonalAccessToken.class);

        query.select(root)
                .where(cb.equal(root.get(PUBLIC_ID), publicId));

        var results = session.createQuery(query)
                .setMaxResults(1)
                .getResultList();

        return results.isEmpty() ? null : results.getFirst();
    }

    @Override
    public List<PersonalAccessToken> getByLoginName(String loginName) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var query = cb.createQuery(PersonalAccessToken.class);
        var root = query.from(PersonalAccessToken.class);

        query.select(root)
                .where(cb.equal(root.get(LOGIN_NAME), loginName))
                .orderBy(cb.desc(root.get("createdAt")));

        return session.createQuery(query).getResultList();
    }

    @Override
    public PersonalAccessToken getByLoginNameAndName(String loginName, String name) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var query = cb.createQuery(PersonalAccessToken.class);
        var root = query.from(PersonalAccessToken.class);

        query.select(root).where(cb.and(cb.equal(root.get(LOGIN_NAME), loginName), cb.equal(root.get("name"), name)));

        var results = session.createQuery(query)
                .setMaxResults(1)
                .getResultList();

        return results.isEmpty() ? null : results.getFirst();
    }

    @Override
    public void deleteByPublicId(String publicId) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();

        var delete = cb.createCriteriaDelete(PersonalAccessToken.class);
        var root = delete.from(PersonalAccessToken.class);

        delete.where(cb.equal(root.get(PUBLIC_ID), publicId));

        session.createMutationQuery(delete).executeUpdate();
    }

    @Override
    public void deleteAllByLoginName(String loginName) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();

        var delete = cb.createCriteriaDelete(PersonalAccessToken.class);
        var root = delete.from(PersonalAccessToken.class);

        delete.where(cb.equal(root.get(LOGIN_NAME), loginName));

        session.createMutationQuery(delete).executeUpdate();
    }


    @Override
    public boolean existsByPublicId(String publicId) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var query = cb.createQuery(Integer.class);
        var root = query.from(PersonalAccessToken.class);

        query.select(cb.literal(1))
                .where(cb.equal(root.get(PUBLIC_ID), publicId));

        var results = session.createQuery(query)
                .setMaxResults(1)
                .getResultList();

        return !results.isEmpty();
    }

}
