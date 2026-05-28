package org.openl.studio.common.projection.test;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO used by the field projection tests. Includes a {@link JsonIgnore} and a
 * {@code WRITE_ONLY} property to verify that projection can never expose normally hidden fields, plus a
 * nested object ({@code owner}) and a nested array of objects ({@code members}) for nested selection.
 */
public class ProjectTestView {

    private final String id;
    private final String name;
    private final String status;

    @JsonIgnore
    private final String secret;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private final String writeOnly;

    private final UserTestView owner;

    private final List<UserTestView> members;

    public ProjectTestView(String id, String name, String status, String secret, String writeOnly,
                           UserTestView owner, List<UserTestView> members) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.secret = secret;
        this.writeOnly = writeOnly;
        this.owner = owner;
        this.members = members;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getSecret() {
        return secret;
    }

    public String getWriteOnly() {
        return writeOnly;
    }

    public UserTestView getOwner() {
        return owner;
    }

    public List<UserTestView> getMembers() {
        return members;
    }
}
