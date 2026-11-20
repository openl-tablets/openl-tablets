package org.openl.studio.settings.model;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The local Maven repository OpenL Studio keeps the downloaded dependencies in.
 *
 * @param size the size of its files, in bytes
 * @author Yury Molchan
 */
@Schema(description = "The local Maven repository the downloaded dependencies are kept in.")
public record MavenCacheModel(@Parameter(description = "Size of the downloaded files, in bytes") long size) {
}
