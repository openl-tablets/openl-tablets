package org.openl.studio.common.projection.test;

/**
 * Nested DTO used to verify that field projection only affects the root type and leaves nested objects
 * fully serialized.
 */
public class UserTestView {

    private final String login;
    private final String email;

    public UserTestView(String login, String email) {
        this.login = login;
        this.email = email;
    }

    public String getLogin() {
        return login;
    }

    public String getEmail() {
        return email;
    }
}
