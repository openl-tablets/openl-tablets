CREATE TABLE OpenL_UserSettings (
    loginName ${varchar}(50) not null,
    settingKey ${varchar}(64) not null,
    settingValue ${varchar}(16) not null,
    PRIMARY KEY (loginName, settingKey),
    CONSTRAINT fk_OpenL_UserSettings1 FOREIGN KEY (loginName) REFERENCES OpenL_Users(loginName) ON DELETE CASCADE
);