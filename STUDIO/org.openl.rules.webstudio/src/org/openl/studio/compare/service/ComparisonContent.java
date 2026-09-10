package org.openl.studio.compare.service;

import java.io.InputStream;

/**
 * A file a comparison is to read, under the name its format is read by.
 *
 * <p>The content is read once, by whoever takes it over, and is closed there.
 */
public record ComparisonContent(String name, InputStream content) {
}
