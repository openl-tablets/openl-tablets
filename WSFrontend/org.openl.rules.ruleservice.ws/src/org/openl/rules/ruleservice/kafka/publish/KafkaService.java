package org.openl.rules.ruleservice.kafka.publish;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.kafka.KafkaHeaders;
import org.openl.rules.ruleservice.kafka.RequestMessage;
import org.openl.rules.ruleservice.logging.ObjectSerializer;
import org.openl.rules.ruleservice.logging.StoreLoggingData;
import org.openl.rules.ruleservice.logging.StoreLoggingDataHolder;
import org.openl.rules.ruleservice.logging.StoreLoggingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KafkaService implements Runnable {

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
        Runtime.getRuntime().availableProcessors() * 2,
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>());

    private final Logger log = LoggerFactory.getLogger(KafkaService.class);

    private volatile boolean flag = true;
    private OpenLService service;
    private String inTopic;
    private String outTopic;
    private String dltTopic;
    private Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private KafkaProducer<String, Object> producer;
    private KafkaProducer<String, byte[]> dltProducer;
    private KafkaConsumer<String, RequestMessage> consumer;
    private Thread loopRunningThread;
    private ObjectSerializer objectSerializer;
    private boolean storeLoggingEnabled;
    private StoreLoggingManager storeLoggingManager;

    public static KafkaService createService(OpenLService service,
            String inTopic,
            String outTopic,
            String dltTopic,
            KafkaConsumer<String, RequestMessage> consumer,
            KafkaProducer<String, Object> producer,
            KafkaProducer<String, byte[]> dltProducer,
            ObjectSerializer objectSerializer,
            StoreLoggingManager storeLoggingManager,
            boolean storeLoggingEnabled) {
        return new KafkaService(service,
            inTopic,
            outTopic,
            dltTopic,
            consumer,
            producer,
            dltProducer,
            objectSerializer,
            storeLoggingManager,
            storeLoggingEnabled);
    }

    private KafkaService(OpenLService service,
            String inTopic,
            String outTopic,
            String dltTopic,
            KafkaConsumer<String, RequestMessage> consumer,
            KafkaProducer<String, Object> producer,
            KafkaProducer<String, byte[]> dltProducer,
            ObjectSerializer objectSerializer,
            StoreLoggingManager storeLoggingManager,
            boolean storeLoggingEnabled) {
        Objects.requireNonNull(service);
        Objects.requireNonNull(inTopic);
        Objects.requireNonNull(producer);
        Objects.requireNonNull(consumer);
        Objects.requireNonNull(dltProducer);
        Objects.requireNonNull(objectSerializer);
        if (storeLoggingEnabled) {
            Objects.requireNonNull(storeLoggingManager);
        }
        this.service = service;
        this.inTopic = inTopic;
        this.outTopic = outTopic;
        this.dltTopic = dltTopic;
        this.consumer = consumer;
        this.producer = producer;
        this.dltProducer = dltProducer;
        this.objectSerializer = objectSerializer;
        this.storeLoggingEnabled = storeLoggingEnabled;
        this.storeLoggingManager = storeLoggingManager;
    }

    public boolean isStoreLoggingEnabled() {
        return storeLoggingEnabled;
    }

    public StoreLoggingManager getStoreLoggingManager() {
        return storeLoggingManager;
    }

    public OpenLService getService() {
        return service;
    }

    public String getInTopic() {
        return inTopic;
    }

    public String getOutTopic(ConsumerRecord<?, ?> record) throws UndefinedTopicException {
        Header header = record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC);
        if (header != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        if (outTopic == null) {
            throw new UndefinedTopicException("Output topic isn't defined.");
        }
        return outTopic;
    }

    public String getDltTopic(ConsumerRecord<?, ?> record) throws UndefinedTopicException {
        Header header = record.headers().lastHeader(KafkaHeaders.REPLY_DLT_TOPIC);
        if (header != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        header = record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC);
        if (header != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        if (dltTopic == null) {
            throw new UndefinedTopicException("Dead letter queue topic isn't defined.");
        }
        return dltTopic;
    }

    private void subscribeConsumer() {
        consumer.subscribe(Collections.singletonList(getInTopic()), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                if (log.isInfoEnabled()) {
                    log.info("Lost partitions in rebalance. Commiting current offsets: " + currentOffsets);
                }
                consumer.commitSync(currentOffsets);
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            }
        });
    }

    private void initialize() {
        loopRunningThread = new Thread(this);
        subscribeConsumer();
    }

    private void runLoop() {
        loopRunningThread.start();
    }

    public void start() throws KafkaServiceException {
        try {
            initialize();
            runLoop();
        } catch (Exception e) {
            throw new KafkaServiceException("Failed to start kafka service.", e);
        }
    }

    public ObjectSerializer getObjectSerializer() {
        return objectSerializer;
    }

    @Override
    public void run() {
        while (flag) {
            try {
                ConsumerRecords<String, RequestMessage> records = consumer.poll(Duration.ofMillis(100));
                if (!records.isEmpty()) {
                    CountDownLatch countDownLatch = new CountDownLatch(records.count());
                    Date incomingTime = new Date();
                    for (ConsumerRecord<String, RequestMessage> consumerRecord : records) {
                        executor.submit(() -> {
                            StoreLoggingData storeLoggingData = isStoreLoggingEnabled() ? StoreLoggingDataHolder.get()
                                                                                        : null;
                            try {
                                if (storeLoggingData != null) {
                                    storeLoggingData.setServiceName(service.getName());
                                    storeLoggingData.setIncomingMessageTime(incomingTime);
                                    storeLoggingData.setPublisherType(PublisherType.KAFKA);
                                    storeLoggingData.setObjectSerializer(getObjectSerializer());
                                    storeLoggingData.setConsumerRecord(consumerRecord);
                                }
                                String outputTopic = getOutTopic(consumerRecord);
                                RequestMessage requestMessage = consumerRecord.value();
                                
                                if (storeLoggingData != null) {
                                    storeLoggingData.setServiceMethod(requestMessage.getMethod());
                                    storeLoggingData.setInputName(requestMessage.getMethod().getName());
                                    storeLoggingData.setParameters(requestMessage.getParameters());
                                }
                                
                                Object result = requestMessage.getMethod()
                                    .invoke(service.getServiceBean(), requestMessage.getParameters());
                                ProducerRecord<String, Object> producerRecord;
                                Header header = consumerRecord.headers().lastHeader(KafkaHeaders.REPLY_PARTITION);
                                if (header == null) {
                                    producerRecord = new ProducerRecord<>(outputTopic, result);
                                } else {
                                    Integer partition = Integer
                                        .valueOf(new String(header.value(), StandardCharsets.UTF_8));
                                    producerRecord = new ProducerRecord<>(outputTopic, partition, null, result);
                                }
                                forwardHeadersToOutput(consumerRecord, producerRecord);
                                if (storeLoggingData != null) {
                                    storeLoggingData.setOutcomingMessageTime(new Date());
                                }
                                producer.send(producerRecord, (metadata, exception) -> {
                                    if (storeLoggingData != null) {
                                        storeLoggingData.setProducerRecord(producerRecord);
                                    }
                                    if (exception != null) {
                                        try {
                                            if (log.isErrorEnabled()) {
                                                log.error(String.format(
                                                    "Failed to send a result message for '%s' method in '%s' service to '%s' output topic.",
                                                    requestMessage.getMethod(),
                                                    getService().getName(),
                                                    getOutTopic(consumerRecord)), exception);
                                            }
                                        } catch (Exception e) {
                                            log.error("Unexpected error.", e);
                                        }
                                        sendErrorToDlt(consumerRecord, exception, storeLoggingData);
                                    } else if (storeLoggingData != null) {
                                        getStoreLoggingManager().submit(storeLoggingData);
                                    }
                                });
                            } catch (Exception e) {
                                if (log.isErrorEnabled()) {
                                    log.error(
                                        String.format("Failed to process a message from '%s' input topic.",
                                            getInTopic()),
                                        e);
                                }
                                sendErrorToDlt(consumerRecord, e, storeLoggingData);
                            } finally {
                                countDownLatch.countDown();
                                if (isStoreLoggingEnabled()) {
                                    StoreLoggingDataHolder.remove();
                                }
                            }
                        });
                    }
                    countDownLatch.await();
                    for (ConsumerRecord<String, RequestMessage> record : records) {
                        currentOffsets.put(new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1));
                    }
                    try {
                        consumer.commitSync(currentOffsets);
                        if (log.isDebugEnabled()) {
                            log.debug("Current offsets have been commited: " + currentOffsets);
                        }
                    } catch (Exception e) {
                        if (log.isErrorEnabled()) {
                            log.error("Failed to commit current offsets: " + currentOffsets);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Something wrong.", e);
            }
        }
    }

    static <T> T exceptionWrappingBlock(Block<T> b) {
        try {
            return b.get();
        } catch (Exception e) {
            return null;
        }
    }

    @FunctionalInterface
    private static interface Block<T> {
        T get() throws Exception;
    }

    private void forwardHeadersToDlt(ConsumerRecord<?, ?> originalRecord, ProducerRecord<?, ?> record) {
        for (Header header : originalRecord.headers()) {
            record.headers().add(header);
        }
    }

    private void forwardHeadersToOutput(ConsumerRecord<?, ?> originalRecord, ProducerRecord<?, ?> record) {
        for (Header header : originalRecord.headers().headers(KafkaHeaders.CORRELATION_ID)) {
            record.headers().add(header);
        }
    }

    private void setDltHeaders(ConsumerRecord<String, RequestMessage> record,
            ProducerRecord<?, ?> dltRecord) throws UnsupportedEncodingException {
        dltRecord.headers()
            .add(KafkaHeaders.DLT_ORIGINAL_MESSAGE_KEY,
                record.key() == null ? null : record.key().getBytes(StandardCharsets.UTF_8));
        dltRecord.headers()
            .add(KafkaHeaders.DLT_ORIGINAL_PARTITION,
                String.valueOf(record.partition()).getBytes(StandardCharsets.UTF_8));
        dltRecord.headers()
            .add(KafkaHeaders.DLT_ORIGINAL_OFFSET, String.valueOf(record.offset()).getBytes(StandardCharsets.UTF_8));
        dltRecord.headers().add(KafkaHeaders.DLT_ORIGINAL_TOPIC, record.topic().getBytes(StandardCharsets.UTF_8));

        dltRecord.headers()
            .add(KafkaHeaders.DLT_EXCEPTION_FQCN,
                record.value().getException().getClass().getName().getBytes(StandardCharsets.UTF_8));
        dltRecord.headers()
            .add(KafkaHeaders.DLT_EXCEPTION_MESSAGE,
                record.value().getException().getMessage() == null ? null
                                                                   : record.value()
                                                                       .getException()
                                                                       .getMessage()
                                                                       .getBytes(StandardCharsets.UTF_8));
        dltRecord.headers()
            .add(KafkaHeaders.DLT_EXCEPTION_STACKTRACE,
                ExceptionUtils.getStackTrace(record.value().getException()).getBytes(StandardCharsets.UTF_8));

        if (record.key() != null) {
            dltRecord.headers()
                .add(KafkaHeaders.DLT_ORIGINAL_MESSAGE_KEY, record.key().getBytes(StandardCharsets.UTF_8));
        }
    }

    private void sendErrorToDlt(ConsumerRecord<String, RequestMessage> record,
            Exception e,
            StoreLoggingData storeLoggingData) {
        final String dltTopic;
        try {
            dltTopic = getDltTopic(record);
        } catch (UndefinedTopicException e1) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Failed to send a message to dead letter queue topic.%sPayload: %s",
                    System.lineSeparator(),
                    record.value().asText()), e1);
            }
            return;
        }
        try {
            ProducerRecord<String, byte[]> dltRecord;
            Header header = record.headers().lastHeader(KafkaHeaders.REPLY_DLT_PARTITION);
            if (header == null) {
                dltRecord = new ProducerRecord<>(dltTopic, record.value().getRawData());
            } else {
                Integer partition = Integer.valueOf(new String(header.value(), StandardCharsets.UTF_8));
                dltRecord = new ProducerRecord<>(dltTopic, partition, null, record.value().getRawData());
            }
            forwardHeadersToDlt(record, dltRecord);
            setDltHeaders(record, dltRecord);
            if (storeLoggingData != null) {
                storeLoggingData.setOutcomingMessageTime(new Date());
            }
            dltProducer.send(dltRecord, (metadata, exception) -> {
                if (storeLoggingData != null) {
                    storeLoggingData.setDltRecord(dltRecord);
                }
                if (exception != null && log.isErrorEnabled()) {
                    log.error(String.format("Failed to send a message to '%s' dead letter queue topic.%sPayload: %s",
                        dltTopic,
                        System.lineSeparator(),
                        record.value().asText()), exception);
                } else if (storeLoggingData != null) {
                    getStoreLoggingManager().submit(storeLoggingData);
                }
            });
        } catch (Exception e1) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Failed to send a message to '%s' dead letter queue topic.%sPayload: %s",
                    dltTopic,
                    System.lineSeparator(),
                    record.value().asText()), e1);
            }
        }
    }

    public void stop() throws InterruptedException {
        flag = false;
        loopRunningThread.join();
    }
}
