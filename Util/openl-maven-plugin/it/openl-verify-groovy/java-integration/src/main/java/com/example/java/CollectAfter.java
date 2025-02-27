package com.example.java;

import java.util.Map;

import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.relation.Relation;

import org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice;

public class CollectAfter implements StoreLogDataAdvice {

    public void prepare(Map<String, Object> map, Object[] objects, Object o, Exception e) {
        QueryBuilder.selectFrom("some_table")
                .column("field3")
                .where(Relation.column("field1").isEqualTo(QueryBuilder.bindMarker()))
                .where(Relation.column("field2").isLessThanOrEqualTo(QueryBuilder.bindMarker()))
                .limit(1)
                .allowFiltering();
    }
}
