package org.openl.rules.webstudio.web.repository.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import lombok.extern.slf4j.Slf4j;
import org.richfaces.model.UploadedFile;

import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

@Slf4j
public class ProjectFile {

    /** The largest upload accepted, applied while a streamed upload is saved to disk. */
    public static final long MAX_UPLOAD_SIZE = 1000L * 1024 * 1024;

    private final String name;
    private InputStream input;
    private long size;
    private File tempFile;

    public ProjectFile(String name, InputStream input) {
        this.name = name;
        this.input = input;
    }

    public ProjectFile(UploadedFile uploadedFile) throws IOException {
        this.name = FileUtils.getName(uploadedFile.getName());
        this.size = uploadedFile.getSize();
        this.tempFile = saveToTempFile(uploadedFile.getInputStream());
        uploadedFile.delete();
    }

    /**
     * Saves the stream to a fresh temporary file. A failed save deletes the partially written file
     * before the error is rethrown.
     */
    private static File saveToTempFile(InputStream input) throws IOException {
        File file = FileUtils.createPrivateTempFile("openl-upload", null).toFile();
        try {
            copyBounded(input, file, MAX_UPLOAD_SIZE);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException suppressed) {
                log.warn("Cannot delete the temporary file {}", file, suppressed);
            }
            throw e;
        }
        return file;
    }

    public String getName() {
        return name;
    }

    public InputStream getInput() throws IOException {
        if (tempFile != null) {
            return new FileInputStream(tempFile);
        } else {
            return input;
        }
    }

    /**
     * Returns the uploaded content as a file on disk, or {@code null} when the file has no content source:
     * it never had one, or it was already {@link #destroy() destroyed}.
     *
     * <p>When the file was created from a stream, the stream is saved to a temporary file on the first
     * call, so the content can be read again later via {@link #getInput()}. A stream longer than
     * {@link #MAX_UPLOAD_SIZE} is rejected while it is being saved, so an oversized upload cannot
     * exhaust the disk.
     */
    public File getTempFile() throws IOException {
        if (tempFile == null && input != null) {
            tempFile = saveToTempFile(input);
            input = null;
            size = tempFile.length();
        }
        return tempFile;
    }

    /**
     * Copies the stream to the file and closes both. Fails as soon as the copied content exceeds
     * {@code maxSize} bytes, leaving the partially written file to the caller.
     */
    static void copyBounded(InputStream input, File file, long maxSize) throws IOException {
        try (input; var output = new FileOutputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            long copied = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                copied += read;
                if (copied > maxSize) {
                    throw new IOException("The uploaded file is larger than " + maxSize + " bytes.");
                }
                output.write(buffer, 0, read);
            }
        }
    }

    public long getSize() {
        return size;
    }

    /**
     * Releases the uploaded content: closes the stream when it was never saved to disk, and deletes the
     * temporary file, if any. When the deletion fails, the failure is logged and the file is kept, so a
     * repeated call can retry.
     */
    public void destroy() {
        if (input != null) {
            IOUtils.closeQuietly(input);
            input = null;
        }
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile.toPath());
                tempFile = null;
            } catch (IOException e) {
                log.warn("Cannot delete the temporary file {}", tempFile, e);
            }
        }
    }
}
