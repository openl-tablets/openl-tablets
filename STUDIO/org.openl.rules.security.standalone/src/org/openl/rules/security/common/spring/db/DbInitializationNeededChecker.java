package org.openl.rules.security.common.spring.db;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.openl.rules.security.standalone.persistence.User;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class DbInitializationNeededChecker extends AbstractFactoryBean<Boolean> {
    private SessionFactory sessionFactory;
    private boolean checkDbIsInitialized = true;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void setCheckDbIsInitialized(boolean checkDbIsInitialized) {
        this.checkDbIsInitialized = checkDbIsInitialized;
    }


    @Override
    public Class<Boolean> getObjectType() {
        return Boolean.class;
    }

    @Override
    protected Boolean createInstance() throws Exception {
        if (!checkDbIsInitialized) {
            return false;
        }

        Session session = null;
        try {
            session = sessionFactory.openSession();
            // If there is no any user, DB is not initialized
            Number rowCount = (Number) session.createCriteria(User.class)
                    .setProjection(Projections.rowCount())
                    .uniqueResult();
            return rowCount.longValue() == 0;
        } catch (RuntimeException e) {
            // Database not initialized
            return true;
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

}
