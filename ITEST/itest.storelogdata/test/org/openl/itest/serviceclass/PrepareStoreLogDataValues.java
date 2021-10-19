package org.openl.itest.serviceclass;

import java.util.Map;

import org.openl.itest.cassandra.HelloEntity8;
import org.openl.itest.elasticsearch.CustomElasticEntity8;
import org.openl.rules.ruleservice.core.interceptors.IOpenClassAware;
import org.openl.rules.ruleservice.core.interceptors.IOpenMemberAware;
import org.openl.rules.ruleservice.storelogdata.ObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataHolder;
import org.openl.rules.ruleservice.storelogdata.advice.ObjectSerializerAware;
import org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.CassandraSession;
import org.openl.rules.ruleservice.storelogdata.elasticsearch.annotation.InjectElasticsearchOperations;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import com.datastax.oss.driver.api.core.CqlSession;

public class PrepareStoreLogDataValues implements StoreLogDataAdvice, ObjectSerializerAware, IOpenClassAware, IOpenMemberAware {

    ObjectSerializer objectSerializer;
    IOpenClass openClass;
    IOpenMember openMember;

    @CassandraSession
    CqlSession cassandraSession;

    @InjectElasticsearchOperations
    ElasticsearchOperations elasticsearchOperations;

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

        values.put("cassandraSessionFound", cassandraSession != null);

        values.put("elasticsearchOperationsFound", elasticsearchOperations != null);

        StoreLogDataHolder.get().ignore(HelloEntity8.class);
        StoreLogDataHolder.get().ignore(CustomElasticEntity8.class);
    }
}
