package org.openl.rules.security.standalone.service;

import org.hibernate.SessionFactory;

import org.hibernate.criterion.Restrictions;

import org.openl.rules.security.standalone.persistence.AccessControlEntry;

public class DefaultAclService {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public AccessControlEntry getAccessControlEntry(final String userName, final String resource) {
        return (AccessControlEntry) sessionFactory.getCurrentSession().createCriteria(AccessControlEntry.class)
                .add(Restrictions.eq("loginName", userName))
                .add(Restrictions.eq("resource", resource))
                .uniqueResult();
    }
}
