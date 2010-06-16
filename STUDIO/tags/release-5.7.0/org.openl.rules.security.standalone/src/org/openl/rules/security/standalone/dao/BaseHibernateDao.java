package org.openl.rules.security.standalone.dao;

import org.openl.rules.security.standalone.persistence.PersistentObject;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import java.util.List;

/**
 * Convenient base implementation of {@link Dao} using Hibernate.
 *
 * @author Andrey Naumenko
 */
public abstract class BaseHibernateDao extends HibernateDaoSupport implements Dao {
    private Class persistentClass;

    public BaseHibernateDao(Class persistentClass) {
        this.persistentClass = persistentClass;
    }

    public boolean canBeDeleted(Object obj) {
        return true;
    }

    public void delete(Object obj) {
        getHibernateTemplate().delete(getHibernateTemplate().load(obj.getClass(), ((PersistentObject) obj).getId()));
    }

    public Object getById(Long id) {
        return getHibernateTemplate().get(persistentClass, id);
    }

    public List loadAll() {
        return getHibernateTemplate().loadAll(persistentClass);
    }

    public Object loadById(Long id) {
        return getHibernateTemplate().load(persistentClass, id);
    }

    public void save(Object obj) {
        getHibernateTemplate().save(obj);
    }

    public void saveOrUpdate(Object obj) {
        getHibernateTemplate().saveOrUpdate(obj);
    }

    public void update(Object obj) {
        getHibernateTemplate().update(obj);
    }
}
