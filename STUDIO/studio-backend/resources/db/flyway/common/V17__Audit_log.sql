CREATE TABLE OpenL_Audit_Log
(
    id             ${identity},
    eventTime      ${timestamp} NOT NULL,
    actor          ${varchar}(100),
    authentication ${varchar}(10),
    clientAddress  ${varchar}(45),
    category       ${varchar}(20) NOT NULL,
    action         ${varchar}(40) NOT NULL,
    outcome        ${varchar}(10) NOT NULL,
    targetType     ${varchar}(20),
    target         ${text},
    project        ${varchar}(2000),
    subjectType    ${varchar}(10),
    subject        ${varchar}(100),
    details        ${text},

    PRIMARY KEY (id)
);

CREATE INDEX ix_OpenL_Audit_Log_eventTime
    ON OpenL_Audit_Log (eventTime);

CREATE INDEX ix_OpenL_Audit_Log_action
    ON OpenL_Audit_Log (action, eventTime);
