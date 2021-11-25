ALTER TABLE OpenL_Users
    ADD email ${varchar}(254);
ALTER TABLE OpenL_Users
    ADD displayName ${varchar}(64);
ALTER TABLE OpenL_Users
    ADD emailVerified ${boolean} default ${false} not null;
ALTER TABLE OpenL_Users
    ADD firstNameExternal ${boolean} default ${false} not null;
ALTER TABLE OpenL_Users
    ADD lastNameExternal ${boolean} default ${false} not null;
ALTER TABLE OpenL_Users
    ADD emailExternal ${boolean} default ${false} not null;
ALTER TABLE OpenL_Users
    ADD displayNameExternal ${boolean} default ${false} not null;
