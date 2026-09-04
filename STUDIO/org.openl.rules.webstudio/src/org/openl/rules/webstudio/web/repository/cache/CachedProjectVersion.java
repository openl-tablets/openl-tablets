package org.openl.rules.webstudio.web.repository.cache;

import java.util.Date;

/**
 * A project version the version cache knows about.
 *
 * @param version the version name it has in its repository
 * @param createdAt when the version was created
 * @param createdBy who created the version
 */
public record CachedProjectVersion(String version, Date createdAt, String createdBy) {
}
