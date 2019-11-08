package org.openl.itest.serviceclass;

import org.openl.itest.cassandra.HelloEntity1;
import org.openl.itest.cassandra.HelloEntity2;
import org.openl.itest.cassandra.HelloEntity3;
import org.openl.itest.elasticsearch.CustomElasticEntity1;
import org.openl.itest.elasticsearch.CustomElasticEntity2;
import org.openl.itest.elasticsearch.CustomElasticEntity3;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor;
import org.openl.rules.ruleservice.storelogdata.annotation.PrepareStoreLogData;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.StoreLogDataToCassandra;
import org.openl.rules.ruleservice.storelogdata.elasticsearch.annotation.StoreLogDataToElasticsearch;

public interface Simple4ServiceAnnotationTemplate {

    @StoreLogDataToCassandra({ HelloEntity1.class, HelloEntity2.class, HelloEntity3.class })
    @StoreLogDataToElasticsearch({ CustomElasticEntity1.class, CustomElasticEntity2.class, CustomElasticEntity3.class })
    @PrepareStoreLogData(PrepareStoreLogDataValue.class)
    @PrepareStoreLogData(PrepareStoreLogDataArgs.class)
    @PrepareStoreLogData(PrepareStoreLogDataResult.class)
    @PrepareStoreLogData(PrepareStoreLogDataObjectSerializerFound.class)
    String Hello(IRulesRuntimeContext runtimeContext, Integer hour);

    @StoreLogDataToCassandra({ HelloEntity1.class })
    @StoreLogDataToElasticsearch({ CustomElasticEntity1.class })
    @ServiceCallBeforeInterceptor(Simple4ServiceMethodBeforeAdvice.class)
    @ServiceCallAfterInterceptor(Simple4ServiceMethodAfterAdvice.class)
    @PrepareStoreLogData(value = BeforeBeforeInterceptor.class, bindToServiceMethodAdvice = Simple4ServiceMethodBeforeAdvice.class, before = true)
    @PrepareStoreLogData(value = AfterBeforeInterceptor.class, bindToServiceMethodAdvice = Simple4ServiceMethodBeforeAdvice.class)
    @PrepareStoreLogData(value = BeforeMethod.class, before = true)
    @PrepareStoreLogData(value = AfterMethod.class)
    @PrepareStoreLogData(value = BeforeAfterInterceptors.class, bindToServiceMethodAdvice = Simple4ServiceMethodAfterAdvice.class, before = true)
    @PrepareStoreLogData(value = AfterAfterInterceptors.class, bindToServiceMethodAdvice = Simple4ServiceMethodAfterAdvice.class)
    String Hello2(IRulesRuntimeContext runtimeContext, Integer hour);
}
