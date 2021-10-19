package org.openl.rules.ruleservice.storelogdata.cassandra;

class CassandraOperationException extends RuntimeException {
    public CassandraOperationException(String message) {
        super(message);
    }
}
