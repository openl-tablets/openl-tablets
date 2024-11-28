CREATE TABLE OPENL_LOCK  (
    LOCK_KEY ${varchar}(36) NOT NULL,
    REGION ${varchar}(100) NOT NULL,
    CLIENT_ID ${varchar}(36),
    CREATED_DATE ${timestamp} NOT NULL,
    constraint OPENL_LOCK_PK primary key (LOCK_KEY, REGION)
);
