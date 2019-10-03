CREATE TABLE openl_logging_hello_entity3 (
    id text,
    incomingtime timestamp,
    methodName text,
    outcomingtime timestamp,
    publishertype text,
    request text,
    response text,
    servicename text,
    hour int,
    value text,
    result text,
    url text,
    PRIMARY KEY (id, publishertype, servicename)
)