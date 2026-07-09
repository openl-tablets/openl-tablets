package org.openl.rules.security.standalone.dao;

import java.util.Locale;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

/**
 * Convenient base implementation of {@link Dao} using Hibernate.
 *
 * @author Andrey Naumenko
 */
public abstract class BaseHibernateDao<T> implements Dao<T> {

    private static final int BATCH_SIZE = 50;

    /** Escape char for SQL LIKE wildcards, exposed for subclasses that build LIKE predicates. */
    protected static final char ESCAPE_CHAR = '\\';

    private SessionFactory sessionFactory;

    @Autowired
    @Qualifier("openlSessionFactory")
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    @Transactional
    public void save(T obj) {
        getSession().persist(obj);
    }

    @Override
    @Transactional
    public void saveOrUpdate(T obj) {
        getSession().merge(obj);
    }

    @Override
    @Transactional
    public void update(T obj) {
        var session = getSession();
        if (!session.contains(obj)) {
            session.merge(obj);
        }
    }

    @Transactional
    @Override
    public void save(Iterable<T> objs) {
        Session session = getSession();
        int i = 0;
        for (T obj : objs) {
            if (i > 0 && i % BATCH_SIZE == 0) {
                //flush a batch of inserts and release memory:
                session.flush();
                session.clear();
            }
            session.persist(obj);
            i++;
        }
    }

    /** Lower-cases a search term and escapes the SQL LIKE wildcards {@code \ [ _ %}. */
    protected static String escape(String searchTerm) {
        return searchTerm.toLowerCase(Locale.ROOT)
                .replace("\\", ESCAPE_CHAR + "\\")
                .replace("[", ESCAPE_CHAR + "[")
                .replace("_", ESCAPE_CHAR + "_")
                .replace("%", ESCAPE_CHAR + "%");
    }
}
