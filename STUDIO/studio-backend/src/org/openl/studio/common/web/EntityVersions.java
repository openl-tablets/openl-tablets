package org.openl.studio.common.web;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * Versions of what a client reads and writes back: a table, a file.
 *
 * <p>A read tells the version in the {@code ETag} header, and a write names the version it is based on in the
 * {@code If-Match} header. A write based on another version than the current one is refused, so that it does not
 * overwrite a change made since the read, on disk or through another client.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityVersions {

    private static final String ANY = "*";

    /**
     * The {@code ETag} header of a version.
     */
    public static String entityTag(String version) {
        return '"' + version + '"';
    }

    /**
     * The version an {@code If-Match} header names, which the current version must equal for the write to be made.
     *
     * <p>No header asks for no check, and neither does {@code *}, which any current version matches: a write of
     * something that is not there is answered as not found before any condition is looked at. The answer is then
     * {@code null}.
     *
     * <p>{@code If-Match} compares tags strongly, so a weak tag matches no version: it is answered as it is, which no
     * version equals. So is a list of several tags. An unquoted tag is taken as it is.
     */
    public static @Nullable String expectedVersion(@Nullable String ifMatch) {
        if (ifMatch == null) {
            return null;
        }
        var tag = ifMatch.trim();
        if (tag.isEmpty() || ANY.equals(tag)) {
            return null;
        }
        var quoted = tag.length() > 1 && tag.startsWith("\"") && tag.endsWith("\"");
        return quoted ? tag.substring(1, tag.length() - 1) : tag;
    }
}
