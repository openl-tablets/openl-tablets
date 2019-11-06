CREATE TABLE IF NOT EXISTS openl_log_data (
    id text,
    incoming_time timestamp,
    method_name text,
    outcoming_time timestamp,
    publisher_type text,
    request text,
    response text,
    service_name text,
    url text,
    PRIMARY KEY (id, publisher_type, service_name)
)