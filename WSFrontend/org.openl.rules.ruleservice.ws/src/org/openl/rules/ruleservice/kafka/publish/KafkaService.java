package org.openl.rules.ruleservice.kafka.publish;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
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
import org.openl.rules.ruleservice.storelogdata.ObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataHolder;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KafkaService implements Runnable {

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
        Runtime.getRuntime().availableProcessors() * 2,
        60L,
        TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());

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
    private StoreLogDataManager storeLogDataManager;

    public static KafkaService createService(OpenLService service,
            String inTopic,
            String outTopic,
            String dltTopic,
            KafkaConsumer<String, RequestMessage> consumer,
            KafkaProducer<String, Object> producer,
            KafkaProducer<String, byte[]> dltProducer,
            ObjectSerializer objectSerializer,
            StoreLogDataManager storeLogDataManager,
            boolean storeLogDataEnabled) {
        return new KafkaService(service,
            inTopic,
            outTopic,
            dltTopic,
            consumer,
            producer,
            dltProducer,
            objectSerializer,
            storeLogDataManager,
            storeLogDataEnabled);
    }

    private KafkaService(OpenLService service,
            String inTopic,
            String outTopic,
            String dltTopic,
            KafkaConsumer<String, RequestMessage> consumer,
            KafkaProducer<String, Object> producer,
            KafkaProducer<String, byte[]> dltProducer,
            ObjectSerializer objectSerializer,
            StoreLogDataManager storeLogDataManager,
            boolean storeLoggingEnabled) {
        this.service = Objects.requireNonNull(service);
        this.inTopic = Objects.requireNonNull(inTopic);
        this.producer = Objects.requireNonNull(producer);
        this.consumer = Objects.requireNonNull(consumer);
        this.dltProducer = Objects.requireNonNull(dltProducer);
        this.objectSerializer = Objects.requireNonNull(objectSerializer);
        if (storeLoggingEnabled) {
            this.storeLogDataManager = Objects.requireNonNull(storeLogDataManager);
        }
        this.outTopic = outTopic;
        this.dltTopic = dltTopic;
        this.storeLoggingEnabled = storeLoggingEnabled;
    }

    public boolean isStoreLogDataEnabled() {
        return storeLoggingEnabled;
    }

    public StoreLogDataManager getStoreLogDataManager() {
        return storeLogDataManager;
    }

    public OpenLService getService() {
        return service;
    }

    public String getInTopic() {
        return inTopic;
    }

    public String getOutTopic(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC);
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return outTopic;
    }

    public String getDltTopic(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader(KafkaHeaders.REPLY_DLT_TOPIC);
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        header = record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC);
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return dltTopic;
    }

    private void subscribeConsumer() {
        consumer.subscribe(Collections.singletonList(getInTopic()), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                if (log.isInfoEnabled()) {
                    log.info("Lost partitions in rebalance. Commiting current offsets: {}", currentOffsets);
                }
                consumer.commitSync(currentOffsets);
                currentOffsets.clear();
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
                    ZonedDateTime incomingTime = ZonedDateTime.now();
                    for (ConsumerRecord<String, RequestMessage> consumerRecord : records) {
                        executor.submit(() -> {
                            StoreLogData storeLogData = isStoreLogDataEnabled() ? StoreLogDataHolder.get() : null;
                            try {
                                if (storeLogData != null) {
                                    storeLogData.setServiceClass(service.getServiceClass());
                                    storeLogData.setServiceName(service.getName());
                                    storeLogData.setIncomingMessageTime(incomingTime);
                                    storeLogData.setPublisherType(PublisherType.KAFKA);
                                    storeLogData.setObjectSerializer(getObjectSerializer());
                                    storeLogData.setConsumerRecord(consumerRecord);
                                }
                                RequestMessage requestMessage = consumerRecord.value();
                                if (storeLogData != null) {
                                    storeLogData.setServiceMethod(requestMessage.getMethod());
                                    storeLogData.setParameters(requestMessage.getParameters());
                                }
                                String outputTopic = getOutTopic(consumerRecord);
                                if (!StringUtils.isBlank(outputTopic)) {
                                    Object result = requestMessage.getMethod()
                                        .invoke(service.getServiceBean(), requestMessage.getParameters());
                                    Header header = consumerRecord.headers().lastHeader(KafkaHeaders.REPLY_PARTITION);
                                    ProducerRecord<String, Object> producerRecord;
                                    if (header == null) {
                                        producerRecord = new ProducerRecord<>(outputTopic,
                                            consumerRecord.key(),
                                            result);
                                    } else {
                                        Integer partition = Integer
                                            .parseInt(new String(header.value(), StandardCharsets.UTF_8));
                                        producerRecord = new ProducerRecord<>(outputTopic,
                                            partition,
                                            consumerRecord.key(),
                                            result);
                                    }
                                    forwardHeadersToOutput(consumerRecord, producerRecord);
                                    if (storeLogData != null) {
                                        storeLogData.setOutcomingMessageTime(ZonedDateTime.now());
                                    }
                                    producer.send(producerRecord, (metadata, exception) -> {
                                        if (storeLogData != null) {
                                            storeLogData.setProducerRecord(producerRecord);
                                        }
                                        if (exception != null) {
                                            try {
                                                if (log.isErrorEnabled()) {
                                                    log.error(String.format(
                                                        "Failed to send a result message for method '%s' in service '%s' to output topic '%s'.",
                                                        requestMessage.getMethod(),
                                                        getService().getName(),
                                                        getOutTopic(consumerRecord)), exception);
                                                }
                                            } catch (Exception e) {
                                                log.error("Unexpected error.", e);
                                            }
                                            sendErrorToDlt(consumerRecord, exception, storeLogData);
                                        } else if (storeLogData != null) {
                                            getStoreLogDataManager().store(storeLogData);
                                        }
                                    });
                                } else {
                                    if (storeLogData != null) {
                                        storeLogData.setOutcomingMessageTime(ZonedDateTime.now());
                                        getStoreLogDataManager().store(storeLogData);
                                    }
                                }
                            } catch (Exception e) {
                                if (log.isErrorEnabled()) {
                                    log.error(
                                        String.format("Failed to process a message from input topic '%s'.",
                                            getInTopic()),
                                        e);
                                }
                                sendErrorToDlt(consumerRecord, e, storeLogData);
                            } finally {
                                countDownLatch.countDown();
                                if (isStoreLogDataEnabled()) {
                                    StoreLogDataHolder.remove();
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
                            log.debug("Current offsets have been committed: {}", currentOffsets);
                        }
                    } catch (Exception e) {
                        if (log.isErrorEnabled()) {
                            log.error("Failed to commit current offsets: {}", currentOffsets);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Something wrong.", e);
            }
        }
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
            Exception e,
            ProducerRecord<?, ?> dltRecord) {
        dltRecord.headers()
            .add(KafkaHeaders.DLT_ORIGINAL_MESSAGE_KEY,
                record.key() == null ? null : record.key().getBytes(StandardCharsets.UTF_8));
        dltRecord.headers()
            .add(KafkaHeaders.DLT_ORIGINAL_PARTITION,
                String.valueOf(record.partition()).getBytes(StandardCharsets.UTF_8));
        dltRecord.headers()
            .add(KafkaHeaders.DLT_ORIGINAL_OFFSET, String.valueOf(record.offset()).getBytes(StandardCharsets.UTF_8));
        dltRecord.headers().add(KafkaHeaders.DLT_ORIGINAL_TOPIC, record.topic().getBytes(StandardCharsets.UTF_8));

        setDltHeadersForException(dltRecord, record.value().getException());

        setDltHeadersForException(dltRecord, e);

        if (record.key() != null) {
            dltRecord.headers()
                .add(KafkaHeaders.DLT_ORIGINAL_MESSAGE_KEY, record.key().getBytes(StandardCharsets.UTF_8));
        }
    }

    private void setDltHeadersForException(ProducerRecord<?, ?> dltRecord, Exception exception) {
        if (exception != null) {
            dltRecord.headers()
                .add(KafkaHeaders.DLT_EXCEPTION_FQCN, exception.getClass().getName().getBytes(StandardCharsets.UTF_8));
            dltRecord.headers()
                .add(KafkaHeaders.DLT_EXCEPTION_MESSAGE,
                    exception.getMessage() == null ? null : exception.getMessage().getBytes(StandardCharsets.UTF_8));
            dltRecord.headers()
                .add(KafkaHeaders.DLT_EXCEPTION_STACKTRACE,
                    ExceptionUtils.getStackTrace(exception).getBytes(StandardCharsets.UTF_8));
        }
    }

    private void sendErrorToDlt(ConsumerRecord<String, RequestMessage> record, Exception e, StoreLogData storeLogData) {
        final String dltTopic = getDltTopic(record);
        if (StringUtils.isEmpty(dltTopic)) {
            return;
        }
        try {
            ProducerRecord<String, byte[]> dltRecord;
            Header header = record.headers().lastHeader(KafkaHeaders.REPLY_DLT_PARTITION);
            if (header == null) {
                dltRecord = new ProducerRecord<>(dltTopic, record.key(), record.value().getRawData());
            } else {
                Integer partition = Integer.parseInt(new String(header.value(), StandardCharsets.UTF_8));
                dltRecord = new ProducerRecord<>(dltTopic, partition, record.key(), record.value().getRawData());
            }
            forwardHeadersToDlt(record, dltRecord);
            setDltHeaders(record, e, dltRecord);
            if (storeLogData != null) {
                storeLogData.setOutcomingMessageTime(ZonedDateTime.now());
            }
            dltProducer.send(dltRecord, (metadata, exception) -> {
                if (storeLogData != null) {
                    storeLogData.setDltRecord(dltRecord);
                    storeLogData.fault();
                }
                if (exception != null && log.isErrorEnabled()) {
                    log.error(String.format("Failed to send a message to dead letter queue topic '%s'.%sPayload: %s",
                        dltTopic,
                        System.lineSeparator(),
                        record.value().asText()), exception);
                } else if (storeLogData != null) {
                    getStoreLogDataManager().store(storeLogData);
                }
            });
        } catch (Exception e1) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Failed to send a message to dead letter queue topic '%s'.%sPayload: %s",
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
