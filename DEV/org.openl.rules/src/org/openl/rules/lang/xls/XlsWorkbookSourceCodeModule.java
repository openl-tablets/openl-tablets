package org.openl.rules.lang.xls;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.load.WorkbookLoader;
import org.openl.rules.lang.xls.load.WorkbookLoaders;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.FileReplacement;
import org.openl.util.FileUtils;
import org.openl.util.HashingUtils;
import org.openl.util.StringTool;

/**
 * @deprecated It will be removed without replacement.
 */
// The engine still represents every loaded workbook with this module; it has no replacement yet.
@SuppressWarnings("java:S1133")
@Deprecated(since = "5.23.10")
@Slf4j
public class XlsWorkbookSourceCodeModule implements IOpenSourceCodeModule {

    /** The digest of a workbook read from another file than the one compiled: no content matches it. */
    private static final String READ_FROM_ANOTHER_FILE = "";

    /** How many times a file that changes while it is read is read again. */
    private static final int READS = 3;

    protected final IOpenSourceCodeModule src;

    @Getter
    private final WorkbookLoader workbookLoader;

    private final Set<Short> wbColors = new TreeSet<>();

    /**
     * What is told when this workbook is saved.
     *
     * <p>Added to while the workbook is being parsed and walked while it is being saved, and those are two
     * threads: a sheet parsed during a save of another sheet's write would otherwise leave the save walking a
     * list that has grown under it. Bought by an iterator that removes nothing, so a listener is taken away
     * with {@code removeIf} rather than through one.
     */
    @Getter
    private final Collection<XlsWorkbookListener> listeners = new CopyOnWriteArrayList<>();

    @Getter
    @Setter
    private Map<String, Object> params;

    /**
     * Whether the workbook was saved since it was read.
     *
     * <p>What was compiled from the workbook as it was read no longer answers for the file then. A compilation reads
     * every workbook again, so a workbook it reads starts unsaved.
     */
    @Getter
    private volatile boolean savedSinceRead;

    /**
     * The file as the workbook was read from it, or as it was last saved. A save refuses to write over a file that
     * differs from it.
     */
    private final AtomicReference<FileVersion> fileVersion;

    /**
     * A workbook read from its file through one snapshot of it.
     *
     * <p>The workbook is read from the whole content of the file read at once, and what it was read from is noted.
     * A save then refuses to overwrite a file that differs from it.
     */
    public XlsWorkbookSourceCodeModule(IOpenSourceCodeModule src) {
        this.src = src;
        fileVersion = new AtomicReference<>(compiled(src));
        workbookLoader = WorkbookLoaders.getWorkbookLoader(new FileSnapshot());
    }

    /**
     * A workbook read by the given loader. A save compares the file with it by the size and the modification time
     * of the file alone.
     */
    public XlsWorkbookSourceCodeModule(IOpenSourceCodeModule src, WorkbookLoader workbookLoader) {
        this.src = src;
        this.workbookLoader = workbookLoader;
        fileVersion = new AtomicReference<>(compiled(src));
    }

    /** The file as it is now, as the compilation is about to read it. */
    private static FileVersion compiled(IOpenSourceCodeModule src) {
        return new FileVersion(stampOf(fileOf(src.getFileUri())), null);
    }

    private void initWorkbookColors() {
        if (!(getWorkbook() instanceof HSSFWorkbook)) {
            return;
        }
        var workbook = getWorkbook();
        var numStyles = workbook.getNumCellStyles();
        for (var i = 0; i < numStyles; i++) {
            var cellStyle = workbook.getCellStyleAt(i);

            wbColors.add(cellStyle.getFillForegroundColor());
            wbColors.add(cellStyle.getFillBackgroundColor());
            wbColors.add(cellStyle.getTopBorderColor());
            wbColors.add(cellStyle.getBottomBorderColor());
            wbColors.add(cellStyle.getLeftBorderColor());
            wbColors.add(cellStyle.getRightBorderColor());
        }

        var numFonts = workbook.getNumberOfFontsAsInt();
        for (var i = 0; i < numFonts; i++) {
            var font = workbook.getFontAt(i);
            wbColors.add(font.getColor());
        }
    }

    public void addListener(XlsWorkbookListener listener) {
        listeners.add(listener);
    }

    public String getDisplayName() {
        String uri = StringTool.decodeURL(src.getUri());
        return FileUtils.getName(uri);
    }

    @Override
    public String getUri() {
        return src.getUri();
    }

    @Override
    public String getFileUri() {
        return src.getFileUri();
    }

    public Workbook getWorkbook() {
        return workbookLoader.getWorkbook();
    }

    /**
     * Refuses to save over a file another program changed since the workbook was read from it, or last saved.
     *
     * <p>The file is compared by its size and its modification time, and by its content too when asked for and the
     * content the workbook was read from is known. A workbook read from another file than the one compiled is
     * refused whatever the file holds. A workbook that had no file, and a file deleted since, are saved.
     *
     * @throws WorkbookChangedOutsideException when the file changed
     */
    private void refuseChangedOutside(File sourceFile, boolean byContent) throws IOException {
        var version = fileVersion.get();
        if (version.stamp() == null || !sourceFile.isFile()) {
            return;
        }
        var readFromAnother = READ_FROM_ANOTHER_FILE.equals(version.digest());
        var contentChanged = byContent && version.digest() != null
                && !version.digest().equals(HashingUtils.sha256Hex(Files.newInputStream(sourceFile.toPath())));
        // The size and the time are compared last, so that a change made while the content is read is told too.
        if (readFromAnother || contentChanged || !version.stamp().equals(stampOf(sourceFile))) {
            throw new WorkbookChangedOutsideException(sourceFile);
        }
    }

    /** The file of the workbook, or {@code null} for a workbook built in memory or read from elsewhere. */
    private static @Nullable File fileOf(@Nullable String fileUri) {
        if (fileUri == null) {
            return null;
        }
        try {
            return new File(new URI(fileUri));
        } catch (URISyntaxException | IllegalArgumentException e) {
            return null;
        }
    }

    private static @Nullable String stampOf(@Nullable File file) {
        return file == null || !file.isFile() ? null : file.length() + ":" + file.lastModified();
    }

    /**
     * The content of a file read at once, with its size and modification time before and after the read.
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Snapshot {
        private final byte[] content;
        private final @Nullable String before;
        private final @Nullable String after;

        static Snapshot of(File file) throws IOException {
            var before = stampOf(file);
            var content = Files.readAllBytes(file.toPath());
            return new Snapshot(content, before, stampOf(file));
        }

        /** Whether the file stayed as it was while it was read. */
        boolean whole() {
            return before != null && before.equals(after);
        }
    }

    /**
     * What the workbook knows of its file.
     *
     * @param stamp  the size and the modification time of the file the compiled tables answer for, or {@code null}
     *               when there was no file
     * @param digest a digest of the content the workbook in memory was read from, {@code null} until it is read, or
     *               {@link #READ_FROM_ANOTHER_FILE}
     */
    private record FileVersion(@Nullable String stamp, @Nullable String digest) {
    }

    /**
     * Synch object for file accessing. It is necessary to prevent getting isModified info before save operation will be
     * finished.
     */
    private final Object fileAccessLock = new Object();

    public File getSourceFile() {
        synchronized (fileAccessLock) {
            File sourceFile = null;
            try {
                var uri = new URI(getFileUri());
                sourceFile = new File(uri);
            } catch (URISyntaxException me) {
                log.debug("Cannot get source file", me);
            }
            return sourceFile;
        }
    }

    /**
     * Saves the workbook into its file.
     *
     * <p>The whole workbook is written beside the file, and replaces it in one step: a reader of the file meets it as
     * it was or as it is saved, never half written, and a save that fails leaves the file as it was. The file is
     * compared with what the workbook was read from before the write, and once more by its content right before it
     * is replaced.
     *
     * @throws WorkbookChangedOutsideException when another program changed the file since it was read
     */
    public void save() throws IOException {
        var sourceFile = getSourceFile();
        synchronized (fileAccessLock) {
            var workbook = getWorkbook();
            refuseChangedOutside(sourceFile, false);
            for (XlsWorkbookListener wl : listeners) {
                wl.beforeSave(this);
            }

            var written = new AtomicReference<String>();
            FileReplacement.replace(sourceFile.toPath(), workbook::write, temporary -> {
                written.set(HashingUtils.sha256Hex(Files.newInputStream(temporary)));
                refuseChangedOutside(sourceFile, true);
            });
            fileVersion.set(new FileVersion(stampOf(sourceFile), written.get()));
            // The file says what the workbook says again, so the workbook may be unloaded and read back from it.
            workbookLoader.setModified(false);
            savedSinceRead = true;

            for (XlsWorkbookListener wl : listeners) {
                wl.afterSave(this);
            }
        }
    }

    /**
     * Saves a change of the look of the workbook alone, such as the formatting of its tables.
     *
     * <p>No value, merge or indent changes, so what was compiled from the workbook still answers for its file: the
     * workbook stays as saved since it was read as it was before.
     *
     * @throws IOException when the file cannot be written
     */
    public void saveLook() throws IOException {
        var saved = savedSinceRead;
        save();
        savedSinceRead = saved;
    }

    public IOpenSourceCodeModule getSource() {
        return src;
    }

    public Set<Short> getWorkbookColors() {
        if (wbColors.isEmpty()) {
            initWorkbookColors();
        }
        return wbColors;
    }

    @Override
    public InputStream getByteStream() {
        return src.getByteStream();
    }

    @Override
    public Reader getCharacterStream() {
        return src.getCharacterStream();
    }

    @Override
    public String getCode() {
        return src.getCode();
    }

    @Override
    public int getStartPosition() {
        return src.getStartPosition();
    }

    /**
     * The source of the workbook as its loader reads it: the content of the file is read at once, and what was read
     * is noted. A source that is no file on disk is read as it is.
     */
    private final class FileSnapshot implements IOpenSourceCodeModule {

        @Override
        public InputStream getByteStream() {
            var file = fileOf(src.getFileUri());
            if (file == null || !file.isFile()) {
                return src.getByteStream();
            }
            try {
                return new ByteArrayInputStream(readSnapshot(file));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /**
         * Reads the whole file at once, again when it changes while it is read, and notes what the workbook is read
         * from.
         */
        private byte[] readSnapshot(File file) throws IOException {
            var read = Snapshot.of(file);
            for (var attempt = 1; attempt < READS && !read.whole(); attempt++) {
                read = Snapshot.of(file);
            }
            var snapshot = read;
            fileVersion.updateAndGet(version -> new FileVersion(version.stamp(), digestOf(version, snapshot)));
            return snapshot.content;
        }

        /**
         * The digest of the content the workbook is read from now.
         *
         * <p>A workbook read again, once it was unloaded, is read from the content it was read from before or last
         * saved. Another program may change the file keeping its size and modification time: the content read then
         * is another file, and the workbook is not saved over it.
         */
        private static String digestOf(FileVersion version, Snapshot snapshot) {
            if (!snapshot.whole() || !snapshot.before.equals(version.stamp())) {
                return READ_FROM_ANOTHER_FILE;
            }
            var digest = HashingUtils.sha256Hex(snapshot.content);
            return version.digest() == null || version.digest().equals(digest) ? digest : READ_FROM_ANOTHER_FILE;
        }

        @Override
        public Reader getCharacterStream() {
            return src.getCharacterStream();
        }

        @Override
        public String getCode() {
            return src.getCode();
        }

        @Override
        public int getStartPosition() {
            return src.getStartPosition();
        }

        @Override
        public String getUri() {
            return src.getUri();
        }

        @Override
        public String getFileUri() {
            return src.getFileUri();
        }

        @Override
        public Map<String, Object> getParams() {
            return src.getParams();
        }

        @Override
        public void setParams(Map<String, Object> params) {
            src.setParams(params);
        }
    }
}
