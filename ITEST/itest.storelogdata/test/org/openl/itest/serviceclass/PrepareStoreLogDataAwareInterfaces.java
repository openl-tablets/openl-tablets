package org.openl.itest.serviceclass;

import java.util.Map;

import org.openl.rules.ruleservice.core.interceptors.IOpenClassAware;
import org.openl.rules.ruleservice.core.interceptors.IOpenMemberAware;
import org.openl.rules.ruleservice.storelogdata.ObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.advice.ObjectSerializerAware;
import org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;

public class PrepareStoreLogDataAwareInterfaces implements StoreLogDataAdvice, ObjectSerializerAware, IOpenClassAware, IOpenMemberAware {

    ObjectSerializer objectSerializer;
    IOpenClass openClass;
    IOpenMember openMember;

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
        values.put("awareInstancesFound", objectSerializer != null && openMember != null && openClass != null);
    }
}
