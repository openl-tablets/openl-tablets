package org.openl.rules.ruleservice.kafka;

public final class KafkaHeaders {
    public static final String PREFIX = "kafka_";

    public static final String METHOD_NAME = "methodName";
    public static final String METHOD_PARAMETERS = "methodParameters";
    public static final String CORRELATION_ID = PREFIX + "correlationId";
    public static final String REPLY_PARTITION = PREFIX + "replyPartition";
    public static final String REPLY_TOPIC = PREFIX + "replyTopic";
    public static final String REPLY_DLT_PARTITION = PREFIX + "replyDltPartition";
    public static final String REPLY_DLT_TOPIC = PREFIX + "replyDltTopic";

    public static final String DLT_EXCEPTION_FQCN = PREFIX + "dlt-exception-fqcn";
    public static final String DLT_EXCEPTION_MESSAGE = PREFIX + "dlt-exception-message";
    public static final String DLT_EXCEPTION_STACKTRACE = PREFIX + "dlt-exception-stacktrace";
    public static final String DLT_ORIGINAL_OFFSET = PREFIX + "dlt-original-offset";
    public static final String DLT_ORIGINAL_PARTITION = PREFIX + "dlt-original-partition";
    public static final String DLT_ORIGINAL_TOPIC = PREFIX + "dlt-original-topic";
    public static final String DLT_ORIGINAL_MESSAGE_KEY = PREFIX + "dlt-original-message-key";

    private KafkaHeaders() {
    }
}
