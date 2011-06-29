package org.openl.rules.security.standalone.dao;

import org.openl.rules.security.standalone.persistence.Group;

/**
 * Hibernate implementation of {@link GroupDao}.
 *
 * @author Andrey Naumenko
 */
public class HibernateGroupDao extends BaseHibernateDao implements GroupDao {
    public HibernateGroupDao() {
        super(Group.class);
    }
}
