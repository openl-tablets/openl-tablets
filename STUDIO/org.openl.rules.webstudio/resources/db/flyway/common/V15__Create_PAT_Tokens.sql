CREATE TABLE OpenL_PAT_Tokens
(
    publicId   ${varchar}(16) NOT NULL,
    secretHash ${varchar}(255) NOT NULL,
    createdAt  ${timestamp} NOT NULL,
    expiresAt  ${timestamp},
    loginName  ${varchar}(50) NOT NULL,
    name       ${varchar}(100) NOT NULL,

    PRIMARY KEY (publicId),

    CONSTRAINT fk_OpenL_PAT_Tokens_user
        FOREIGN KEY (loginName)
            REFERENCES OpenL_Users(loginName)
            ON DELETE CASCADE,

    CONSTRAINT uq_OpenL_PAT_Tokens_user_name
        UNIQUE (loginName, name),

    CONSTRAINT ck_OpenL_PAT_Tokens_expires
        CHECK (expiresAt IS NULL OR expiresAt > createdAt)
);

CREATE INDEX ix_OpenL_PAT_Tokens_loginName
    ON OpenL_PAT_Tokens (loginName);
