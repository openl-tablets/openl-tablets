package org.openl.itest.serviceclass;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.StoreLogDataToCassandra;
import org.openl.rules.ruleservice.storelogdata.elasticsearch.annotation.StoreLogDataToElasticsearch;

@StoreLogDataToCassandra
@StoreLogDataToElasticsearch
public interface Simple3ServiceAnnotationTemplate {

    String Hello(IRulesRuntimeContext runtimeContext, Integer hour);
}
