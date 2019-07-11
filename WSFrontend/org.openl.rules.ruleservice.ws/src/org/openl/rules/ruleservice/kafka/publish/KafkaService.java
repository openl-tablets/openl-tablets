package org.openl.rules.ruleservice.kafka.publish;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.kafka.ser.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KafkaService implements Runnable {

    private static final String UTF8 = "UTF8";

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
    private KafkaConsumer<String, Message> consumer;
    private Thread loopRunningThread;

    public static KafkaService createService(OpenLService service,
            String inTopic,
            String outTopic,
            String dltTopic,
            KafkaConsumer<String, Message> consumer,
            KafkaProducer<String, Object> producer,
            KafkaProducer<String, byte[]> dltProducer) {
        return new KafkaService(service, inTopic, outTopic, dltTopic, consumer, producer, dltProducer);
    }

    private KafkaService(OpenLService service,
            String inTopic,
            String outTopic,
            String dltTopic,
            KafkaConsumer<String, Message> consumer,
            KafkaProducer<String, Object> producer,
            KafkaProducer<String, byte[]> dltProducer) {
        this.service = service;
        this.inTopic = inTopic;
        this.outTopic = outTopic;
        this.dltTopic = dltTopic;
        this.consumer = consumer;
        this.producer = producer;
        this.dltProducer = dltProducer;
    }

    public OpenLService getService() {
        return service;
    }

    public String getInTopic() {
        return inTopic;
    }

    public String getOutTopic(ConsumerRecord<?, ?> record) throws UnsupportedEncodingException {
        Header header = record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC);
        if (header != null) {
            return new String(header.value(), UTF8);
        }
        if (outTopic == null) {
            throw new DltTopicIsNotDefinedException("Output topic is not defined.");
        }
        return outTopic;
    }

    public String getDltTopic(ConsumerRecord<?, ?> record) throws UnsupportedEncodingException {
        Header header = record.headers().lastHeader(KafkaHeaders.REPLY_DLT_TOPIC);
        if (header != null) {
            return new String(header.value(), UTF8);
        }
        if (dltTopic == null) {
            throw new DltTopicIsNotDefinedException("DLT topic is not defined.");
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

    @Override
    public void run() {
        while (flag) {
            try {
                ConsumerRecords<String, Message> records = consumer.poll(Duration.ofMillis(100));
                if (!records.isEmpty()) {
                    CountDownLatch countDownLatch = new CountDownLatch(records.count());
                    for (ConsumerRecord<String, Message> record : records) {
                        executor.submit(() -> {
                            try {
                                String outputTopic = getOutTopic(record);
                                Message message = record.value();
                                Object result = message.getMethod()
                                    .invoke(service.getServiceBean(), message.getParameters());
                                ProducerRecord<String, Object> producerRecord;
                                Header header = record.headers().lastHeader(KafkaHeaders.REPLY_PARTITION);
                                if (header == null) {
                                    producerRecord = new ProducerRecord<>(outputTopic, result);
                                } else {
                                    Integer partition = Integer.valueOf(new String(header.value(), UTF8));
                                    producerRecord = new ProducerRecord<>(outputTopic, partition, null, result);
                                }
                                forwardHeadersToOutput(record, producerRecord);
                                producer.send(producerRecord, (metadata, exception) -> {
                                    if (exception != null) {
                                        try {
                                            if (log.isErrorEnabled()) {
                                                log.error(String.format(
                                                    "Failed to send a result message for '%s' method in '%s' service to '%s' output topic.",
                                                    message.getMethod(),
                                                    getService().getName(),
                                                    getOutTopic(record)), exception);
                                            }
                                        } catch (Exception e) {
                                            log.error("Unexpected error.", e);
                                        }
                                        sendErrorToDlt(record, exception);
                                    }
                                });
                            } catch (Exception e) {
                                if (log.isErrorEnabled()) {
                                    log.error(
                                        String.format("Failed to process a message from '%s' input topic.",
                                            getInTopic()),
                                        e);
                                }
                                sendErrorToDlt(record, e);
                            } finally {
                                countDownLatch.countDown();
                            }
                        });
                    }
                    countDownLatch.await();
                    for (ConsumerRecord<String, Message> record : records) {
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

    private void setDltHeaders(ConsumerRecord<String, Message> record,
            ProducerRecord<?, ?> dltRecord) throws UnsupportedEncodingException {
        dltRecord.headers()
            .add(KafkaHeaders.DLT_ORIGINAL_MESSAGE_KEY, record.key() == null ? null : record.key().getBytes(UTF8));
        dltRecord.headers().add(KafkaHeaders.DLT_ORIGINAL_PARTITION, String.valueOf(record.partition()).getBytes(UTF8));
        dltRecord.headers().add(KafkaHeaders.DLT_ORIGINAL_OFFSET, String.valueOf(record.offset()).getBytes(UTF8));
        dltRecord.headers().add(KafkaHeaders.DLT_ORIGINAL_TOPIC, record.topic().getBytes(UTF8));

        dltRecord.headers()
            .add(KafkaHeaders.DLT_EXCEPTION_FQCN, record.value().getException().getClass().getName().getBytes(UTF8));
        dltRecord.headers()
            .add(KafkaHeaders.DLT_EXCEPTION_MESSAGE,
                record.value().getException().getMessage() == null ? null
                                                                   : record.value()
                                                                       .getException()
                                                                       .getMessage()
                                                                       .getBytes(UTF8));
        dltRecord.headers()
            .add(KafkaHeaders.DLT_EXCEPTION_STACKTRACE,
                ExceptionUtils.getStackTrace(record.value().getException()).getBytes(UTF8));

        if (record.key() != null) {
            dltRecord.headers().add(KafkaHeaders.DLT_ORIGINAL_MESSAGE_KEY, record.key().getBytes(UTF8));
        }
    }

    private void sendErrorToDlt(ConsumerRecord<String, Message> record, Exception e) {
        String topic = null;
        try {
            String dltTopic = getDltTopic(record);
            topic = dltTopic;
            ProducerRecord<String, byte[]> dltRecord;
            Header header = record.headers().lastHeader(KafkaHeaders.REPLY_DLT_PARTITION);
            if (header == null) {
                dltRecord = new ProducerRecord<>(dltTopic, record.value().getRawData());
            } else {
                Integer partition = Integer.valueOf(new String(header.value(), UTF8));
                dltRecord = new ProducerRecord<>(dltTopic, partition, null, record.value().getRawData());
            }
            forwardHeadersToDlt(record, dltRecord);
            setDltHeaders(record, dltRecord);
            dltProducer.send(dltRecord, (metadata, exception) -> {
                if (exception != null && log.isErrorEnabled()) {
                    log.error(String.format("Failed to send a message to '%s' dead letter topic.%sPayload: %s",
                        dltTopic,
                        System.lineSeparator(),
                        record.value().asText()), exception);
                }
            });
        } catch (DltTopicIsNotDefinedException | UnsupportedEncodingException e1) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Failed to send a message to dead letter topic.%sPayload: %s",
                    System.lineSeparator(),
                    record.value().asText()), e1);
            }
        } catch (Exception e1) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Failed to send a message to '%s' dead letter topic.%sPayload: %s",
                    topic,
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
