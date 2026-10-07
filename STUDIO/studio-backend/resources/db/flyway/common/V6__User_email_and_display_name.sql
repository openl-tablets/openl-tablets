ALTER TABLE OpenL_Users
    ADD email ${varchar}(254);
ALTER TABLE OpenL_Users
    ADD displayName ${varchar}(64);
ALTER TABLE OpenL_Users
    ADD flags ${int} default 0 not null;
