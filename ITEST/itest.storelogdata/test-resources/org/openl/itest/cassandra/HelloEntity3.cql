CREATE TABLE openl_logging_hello_entity3 (
    id text,
    method_name text,
    incoming_time timestamp,
    outcoming_time timestamp,
    publisher_type text,
    request text,
    response text,
    service_name text,
    hour int,
    value text,
    result text,
    url text,
    PRIMARY KEY (id, publisher_type, service_name)
)