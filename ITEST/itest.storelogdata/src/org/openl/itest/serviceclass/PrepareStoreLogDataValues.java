package org.openl.itest.serviceclass;

import java.util.Map;
import jakarta.persistence.EntityManager;

import org.openl.rules.ruleservice.core.interceptors.IOpenClassAware;
import org.openl.rules.ruleservice.core.interceptors.IOpenMemberAware;
import org.openl.rules.ruleservice.storelogdata.ObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataHolder;
import org.openl.rules.ruleservice.storelogdata.advice.ObjectSerializerAware;
import org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice;
import org.openl.rules.ruleservice.storelogdata.db.annotation.InjectEntityManager;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;

public class PrepareStoreLogDataValues implements StoreLogDataAdvice, ObjectSerializerAware, IOpenClassAware, IOpenMemberAware {

    ObjectSerializer objectSerializer;
    IOpenClass openClass;
    IOpenMember openMember;

    @InjectEntityManager
    EntityManager entityManager;

    @Override
    public void setObjectSerializer(ObjectSerializer objectSerializer) {
        this.objectSerializer = objectSerializer;
    }

    @Override
    public void setIOpenClass(IOpenClass openClass) {
        this.openClass = openClass;
    }

    @Override
    public void setIOpenMember(IOpenMember openMember) {
        this.openMember = openMember;
    }

    @Override
    public void prepare(Map<String, Object> values, Object[] args, Object result, Exception ex) {
        values.put("value1", "value1");
        values.put("hour", args[1]);
        values.put("result", result);

        values.put("awareInstancesFound", objectSerializer != null && openMember != null && openClass != null);

        values.put("dbConnectionFound", entityManager != null && entityManager.isOpen());

        StoreLogDataHolder.get().ignore(org.openl.itest.db.HelloEntity8.class);
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
