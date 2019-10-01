package org.openl.itest.serviceclass;

import org.openl.itest.cassandra.HelloEntity1;
import org.openl.itest.cassandra.HelloEntity2;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.logging.cassandra.annotation.CassandraEntity;

public interface Simple4ServiceAnnotationTemplate {

    @CassandraEntity({ HelloEntity1.class, HelloEntity2.class })
    String Hello(IRulesRuntimeContext runtimeContext, Integer hour);
}

