package org.openl.rules.ruleservice.kafka.publish;

public interface KafkaHeaders {
    String PREFIX = "kafka_";

    String METHOD_NAME = "methodName";
    String METHOD_PARAMETERS = "methodParameters";
    String CORRELATION_ID = PREFIX + "correlationId";
    String REPLY_PARTITION = PREFIX + "replyPartition";
    String REPLY_TOPIC = PREFIX + "replyTopic";
    String REPLY_DLT_PARTITION = PREFIX + "replyDltPartition";
    String REPLY_DLT_TOPIC = PREFIX + "replyDltTopic";

    String DLT_EXCEPTION_FQCN = PREFIX + "dlt-exception-fqcn";
    String DLT_EXCEPTION_MESSAGE = PREFIX + "dlt-exception-message";
    String DLT_EXCEPTION_STACKTRACE = PREFIX + "dlt-exception-stacktrace";
    String DLT_ORIGINAL_OFFSET = PREFIX + "dlt-original-offset";
    String DLT_ORIGINAL_PARTITION = PREFIX + "dlt-original-partition";
    String DLT_ORIGINAL_TOPIC = PREFIX + "dlt-original-topic";
    String DLT_ORIGINAL_MESSAGE_KEY = PREFIX + "dlt-original-message-key";
}
