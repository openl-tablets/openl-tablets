package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.persistence.UserSetting;
import org.openl.rules.security.standalone.persistence.UserSettingId;

public class HibernateUserSettingDao extends BaseHibernateDao<UserSetting> implements UserSettingDao {

    @Override
    @Transactional
    public UserSetting getProperty(String login, String key) {
        return findProperty(login, key);
    }

    @Override
    @Transactional
    public void setProperty(String login, String key, String value) {
        var session = getSession();
        var property = findProperty(login, key);
        if (property == null) {
            property = new UserSetting();
            property.setId(new UserSettingId(login, key));
            property.setSettingValue(value);
            session.persist(property);
        } else {
            property.setSettingValue(value);
            if (!session.contains(property)) {
                session.merge(property);
            }
        }
    }

    private UserSetting findProperty(String login, String key) {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(UserSetting.class);
        var u = criteria.from(UserSetting.class);
        criteria.select(u)
                .where(builder.and(builder.equal(u.get("id").get("loginName"), login),
                        builder.equal(u.get("id").get("settingKey"), key)))
                .distinct(true);
        List<UserSetting> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.getFirst();
    }

    @Override
    @Transactional
    public void removeProperty(String login, String key) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var delete = cb.createCriteriaDelete(UserSetting.class);
        var root = delete.from(UserSetting.class);
        delete.where(cb.and(
                cb.equal(root.get("id").get("loginName"), login),
                cb.equal(root.get("id").get("settingKey"), key)));
        session.createMutationQuery(delete).executeUpdate();
    }
}
