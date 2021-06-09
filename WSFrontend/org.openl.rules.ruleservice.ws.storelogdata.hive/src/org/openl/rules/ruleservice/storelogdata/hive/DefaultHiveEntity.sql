CREATE TABLE IF NOT EXISTS openl_log_data (
    id String,
    incomingTime timestamp,
    methodName String,
    outcomingTime timestamp,
    publisherType String,
    request String,
    response String,
    serviceName String,
    url String,
    PRIMARY KEY (id, publisherType, serviceName)  disable novalidate
)