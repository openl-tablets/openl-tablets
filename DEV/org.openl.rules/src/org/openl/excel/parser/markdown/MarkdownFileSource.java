package org.openl.excel.parser.markdown;

import java.nio.file.Path;

import org.openl.source.impl.PathSourceCodeModule;

/**
 * A file of a module folder, known by the address of the module followed by the path of the file in the folder.
 *
 * <p>The address of the module is the one its tables are told apart by, so the tables of a file keep the same
 * address wherever the project is. The file itself is read from where it lies.
 */
public final class MarkdownFileSource extends PathSourceCodeModule {

    /** The address of the module followed by the path of the file in the folder. */
    private final String moduleUri;

    /**
     * @param file the file in the file system
     * @param uri  the address of the module followed by the path of the file in the folder
     */
    public MarkdownFileSource(Path file, String uri) {
        super(file);
        moduleUri = uri;
    }

    /** The address the file is known by, which {@link #getUri()} answers. */
    @Override
    protected String makeUri() {
        return moduleUri;
    }

    /** The address of the file in the file system. */
    @Override
    public String getFileUri() {
        return super.makeUri();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MarkdownFileSource that && moduleUri.equals(that.moduleUri);
    }

    @Override
    public int hashCode() {
        return moduleUri.hashCode();
    }

    @Override
    public String toString() {
        return getFileUri();
    }
}
