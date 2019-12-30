package org.openl.rules.security.standalone.dao;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.openl.rules.security.standalone.persistence.UserSetting;
import org.openl.rules.security.standalone.persistence.UserSettingId;
import org.springframework.transaction.annotation.Transactional;

public class HibernateUserSettingDao extends BaseHibernateDao<UserSetting> implements UserSettingDao {

    @Override
    @Transactional
    public UserSetting getProperty(String login, String key) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<UserSetting> criteria = builder.createQuery(UserSetting.class);
        Root<UserSetting> u = criteria.from(UserSetting.class);
        criteria.select(u)
            .where(builder.and(builder.equal(u.get("id").get("loginName"), login),
                builder.equal(u.get("id").get("settingKey"), key)))
            .distinct(true);
        List<UserSetting> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    @Transactional
    public void setProperty(String login, String key, String value) {
        UserSetting property = getProperty(login, key);
        if (property == null) {
            property = new UserSetting();
            property.setId(new UserSettingId(login, key));
            property.setSettingValue(value);
            save(property);
        } else {
            property.setSettingValue(value);
            update(property);
        }
    }

    @Override
    @Transactional
    public void removeProperty(String login, String key) {
        getSession().createNativeQuery("delete from OpenL_UserSettings where loginName = :name and settingKey = :key")
            .setParameter("name", login)
            .setParameter("key", key)
            .executeUpdate();
    }
}
