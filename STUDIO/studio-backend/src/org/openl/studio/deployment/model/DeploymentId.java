package org.openl.studio.deployment.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The id a deployment is addressed by: the deployment repository and the name of the deployment.
 *
 * <p>It is written as the URL-safe Base64 of {@code repository:name}, read as UTF-8, so it stays within one URL path
 * segment. See the module {@code AGENTS.md}, section "Ids in a URL Path".
 *
 * @param repository the id of the deployment repository
 * @param name       the name of the deployment
 */
public record DeploymentId(String repository, String name) {

    private static final String ID_SEPARATOR = ":";

    /**
     * The id as a URL path segment takes it.
     */
    @JsonValue
    public String encode() {
        var src = repository + ID_SEPARATOR + name;
        return Base64.getUrlEncoder().encodeToString(src.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The id written as the given text.
     *
     * @throws IllegalArgumentException when the text is not an id
     */
    @JsonCreator
    public static DeploymentId decode(String encoded) {
        // Both alphabets are accepted: ids issued before the URL-safe form became the default still live in
        // bookmarks. A standard id never contains '-'/'_', so this mapping is a no-op for it.
        var normalized = encoded.replace('-', '+').replace('_', '/');
        var decoded = new String(Base64.getDecoder().decode(normalized), StandardCharsets.UTF_8);
        var separator = decoded.indexOf(ID_SEPARATOR);
        if (separator == -1) {
            throw new IllegalArgumentException("Invalid deployment id: " + encoded);
        }
        return new DeploymentId(decoded.substring(0, separator), decoded.substring(separator + 1));
    }
}
