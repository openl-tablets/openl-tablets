package org.openl.studio.projects.service.localization;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.openl.studio.common.exception.ConflictException;
import org.openl.util.HashingUtils;

/**
 * The changes of one localization file, made one after another and then written at once.
 *
 * <p>A text of a key the file holds is written over the key where it stands. A key the file lacks is added at the
 * end. A key taken out of the file leaves no line behind: every line it was written on goes, a key written twice
 * included.
 *
 * <p>Only what changed is written anew: the file from the first message changed on, or, when keys are only added,
 * the new lines at its end; the part before is copied as it is. A file in a legacy encoding, and a new file, are
 * written whole, in UTF-8.
 *
 * <p>The file is written beside itself and takes the place of the file in one step, so a failure leaves either the
 * file as it was or the file as the changes leave it, never a part of it. A file that changed since it was read is not
 * written over.
 */
final class MessageFileEdit {

    private final @Nullable MessageFile file;
    private final Path path;
    private final String lineBreak;
    /** The separator a message added to the file takes, as the file writes its messages. */
    private final String separator;
    /** The keys the file holds, the last message of each standing for it. */
    private final Map<String, MessageFile.Message> held = new LinkedHashMap<>();
    private final Set<String> removed = new HashSet<>();
    /** The new texts of messages the file holds, by the message written over. */
    private final Map<MessageFile.Message, @Nullable String> rewritten = new LinkedHashMap<>();
    /** The keys added at the end, in the order they were added. */
    private final Map<String, @Nullable String> added = new LinkedHashMap<>();

    /** The changes of a file as it was read. */
    MessageFileEdit(MessageFile file) {
        this.file = file;
        this.path = file.path();
        this.lineBreak = file.lineBreak();
        this.separator = file.separator();
        file.messages().forEach(message -> held.put(message.key(), message));
    }

    /** The changes of a file that does not exist yet: it is created with what is added to it. */
    MessageFileEdit(Path path) {
        this.file = null;
        this.path = path;
        this.lineBreak = "\n";
        this.separator = MessageFile.DEFAULT_SEPARATOR;
    }

    /** Where the file is. */
    Path path() {
        return path;
    }

    /** Whether the file holds the key, as the changes made so far leave it. */
    boolean has(String key) {
        return added.containsKey(key) || held.containsKey(key) && !removed.contains(key);
    }

    /** Writes the text of a key: over the key where the file holds it, or at the end of the file. */
    void set(String key, @Nullable String text) {
        if (added.containsKey(key) || !held.containsKey(key) || removed.contains(key)) {
            added.put(key, text);
        } else {
            rewritten.put(held.get(key), text);
        }
    }

    /** Takes a key out of the file, every line it is written on. */
    void remove(String key) {
        added.remove(key);
        var message = held.get(key);
        if (message != null) {
            rewritten.remove(message);
            removed.add(key);
        }
    }

    /** Whether anything is to be written. */
    boolean changed() {
        return file == null || !added.isEmpty() || !removed.isEmpty() || !rewritten.isEmpty();
    }

    /**
     * Writes the file as the changes leave it beside it, in a file of its own that takes its place only once
     * {@link Aside#replace()} is called, so a failure before then leaves the file as it was.
     *
     * <p>The part of the file before the first message changed is copied as it is; only the rest is written anew. A
     * file in a legacy encoding, and a new file, are written whole, in UTF-8.
     *
     * @return the file written aside
     * @throws ConflictException when the file changed since it was read
     */
    Aside writeAside() throws IOException {
        requireUnchanged();
        var folder = path.getParent();
        Files.createDirectories(folder);
        var aside = Files.createFile(beside(".tmp"));
        try {
            if (file == null || file.legacy()) {
                writeWhole(file, aside);
            } else {
                writeChanged(file, aside);
            }
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(aside);
            throw e;
        }
        return new Aside(aside);
    }

    /**
     * Refuses to write over a file that changed since it was read, by anything: its messages stand elsewhere now.
     *
     * @throws ConflictException when the file changed, or a new file was created meanwhile
     */
    void requireUnchanged() {
        var now = MessageFile.Stamp.of(path);
        // A write of the same size in the same tick of the clock of the file system is told by the content.
        var unchanged = file == null ? now == null : file.stamp().equals(now) && file.digest().equals(digestOf(path));
        if (!unchanged) {
            throw new ConflictException("localization.file.changed.message", path.getFileName().toString());
        }
    }

    /**
     * Puts every file written aside in the place of its file, all or none.
     *
     * <p>A file that cannot take its place puts back the files replaced before it, as they were, and the failure is
     * told. A file that cannot be put back is told with the failure.
     *
     * @param asides the files written aside, of one change of the messages
     */
    static void replaceAll(List<Aside> asides) throws IOException {
        var replaced = new ArrayList<Replaced>();
        try {
            for (var aside : asides) {
                replaced.add(aside.replace());
            }
        } catch (IOException | RuntimeException e) {
            replaced.reversed().forEach(done -> done.undo(e));
            throw e;
        }
        replaced.forEach(Replaced::forget);
    }

    /** A path beside the file, named apart from the localization files, which never read it. */
    private Path beside(String suffix) {
        return path.resolveSibling("." + path.getFileName() + "." + UUID.randomUUID() + suffix);
    }

    /** Moves a file over another, in one step where the file system can. */
    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** The digest of the content of the file, or nothing for a file that cannot be read. */
    private static String digestOf(Path path) {
        try {
            return HashingUtils.sha256Hex(Files.readAllBytes(path));
        } catch (IOException e) {
            return "";
        }
    }

    /** A file written beside the one it replaces. */
    final class Aside {

        private final Path written;

        private Aside(Path written) {
            this.written = written;
        }

        /**
         * Puts the file written aside in the place of the file, keeping a copy of the file it replaces until the
         * change of the messages is done.
         */
        Replaced replace() throws IOException {
            var kept = Files.isRegularFile(path) ? Files.copy(path, beside(".bak")) : null;
            var replaced = new Replaced(kept);
            try {
                move(written, path);
            } catch (IOException | RuntimeException e) {
                replaced.forget();
                throw e;
            }
            return replaced;
        }

        /** Deletes the file written aside, unless it took the place of the file. */
        void discard() {
            try {
                Files.deleteIfExists(written);
            } catch (IOException e) {
                // A file left over is named apart from the localization files, which never read it.
            }
        }
    }

    /** A file put in the place of the file, with a copy of what it replaced. */
    final class Replaced {

        private final @Nullable Path kept;

        private Replaced(@Nullable Path kept) {
            this.kept = kept;
        }

        /**
         * Puts the file back as it was: the copy kept, or no file where there was none.
         *
         * @param failure what made the change fail, which is told a file that cannot be put back too
         */
        void undo(Exception failure) {
            try {
                if (kept == null) {
                    Files.deleteIfExists(path);
                } else {
                    move(kept, path);
                }
            } catch (IOException e) {
                failure.addSuppressed(e);
            }
        }

        /** Lets go of the copy kept, once the change is done. */
        void forget() {
            if (kept != null) {
                try {
                    Files.deleteIfExists(kept);
                } catch (IOException e) {
                    // A copy left over is named apart from the localization files, which never read it.
                }
            }
        }
    }

    /** Writes the whole file in UTF-8: a new file, or one read in a legacy encoding. */
    private void writeWhole(@Nullable MessageFile read, Path target) throws IOException {
        var out = new ByteArrayOutputStream();
        if (read != null) {
            var text = new String(Files.readAllBytes(path), MessageFile.WINDOWS_1252);
            var at = 0;
            for (var change : changesOf(read)) {
                out.writeBytes(text.substring(at, (int) change.start()).getBytes(StandardCharsets.UTF_8));
                out.writeBytes(change.bytes());
                at = (int) change.end();
            }
            out.writeBytes(text.substring(at).getBytes(StandardCharsets.UTF_8));
        }
        var written = out.toByteArray();
        out.writeBytes(appended(written.length > 0 ? written[written.length - 1] : null));
        Files.write(target, out.toByteArray());
    }

    /**
     * Writes a UTF-8 file as the changes leave it: the part before the first message changed copied as it is, the
     * rest written anew, then the lines added.
     */
    private void writeChanged(MessageFile read, Path target) throws IOException {
        var changes = changesOf(read);
        try (var source = FileChannel.open(path, StandardOpenOption.READ);
             var out = FileChannel.open(target, StandardOpenOption.WRITE)) {
            var length = source.size();
            var from = changes.isEmpty() ? length : changes.getFirst().start();
            copyHead(source, out, from);
            var tail = ByteBuffer.allocate((int) (length - from));
            readFully(source, tail, from);
            var rest = new ByteArrayOutputStream();
            var at = from;
            for (var change : changes) {
                rest.write(tail.array(), (int) (at - from), (int) (change.start() - at));
                rest.writeBytes(change.bytes());
                at = change.end();
            }
            rest.write(tail.array(), (int) (at - from), (int) (length - at));
            var written = rest.toByteArray();
            var last = written.length > 0 ? Byte.valueOf(written[written.length - 1]) : lastByteBefore(source, from);
            out.position(from);
            writeFully(out, written);
            writeFully(out, appended(last));
        }
    }

    /** Copies the part of the file before the first change as it is, without reading it into memory. */
    private static void copyHead(FileChannel source, FileChannel out, long count) throws IOException {
        var copied = 0L;
        while (copied < count) {
            var step = source.transferTo(copied, count - copied, out);
            if (step <= 0) {
                throw new EOFException("The file ended while it was copied: " + count + " bytes expected");
            }
            copied += step;
        }
    }

    private static void readFully(FileChannel source, ByteBuffer buffer, long at) throws IOException {
        while (buffer.hasRemaining()) {
            if (source.read(buffer, at + buffer.position()) < 0) {
                throw new EOFException("The file ended while it was read");
            }
        }
    }

    private static void writeFully(FileChannel out, byte[] bytes) throws IOException {
        var buffer = ByteBuffer.wrap(bytes);
        while (buffer.hasRemaining()) {
            out.write(buffer);
        }
    }

    /**
     * The places of the file to write anew, in the order of the file: a message rewritten keeps its line break, a
     * message removed goes with it.
     */
    private List<Change> changesOf(MessageFile file) {
        var changes = new ArrayList<Change>();
        rewritten.forEach((message, text) -> changes.add(new Change(message.start(),
                message.contentEnd(),
                PropertiesText.line(message.key(), text, separatorOf(message)))));
        file.messages()
                .stream()
                .filter(message -> removed.contains(message.key()))
                .forEach(message -> changes.add(new Change(message.start(), message.end(), "")));
        changes.sort(Comparator.comparingLong(Change::start));
        return changes;
    }

    /** The separator a message written anew keeps: its own, or that of the file for a line written without one. */
    private String separatorOf(MessageFile.Message message) {
        return message.separator().isEmpty() ? separator : message.separator();
    }

    /**
     * The lines of the keys added, each ending with a line break, which go at the end of the file.
     *
     * @param last the last byte the file has before them, {@code null} for an empty file: a line not ended yet is
     *             ended first
     */
    private byte[] appended(@Nullable Byte last) {
        if (added.isEmpty()) {
            return new byte[0];
        }
        var lines = new StringBuilder();
        if (last != null && last != '\n' && last != '\r') {
            lines.append(lineBreak);
        }
        added.forEach((key, text) -> lines.append(PropertiesText.line(key, text, separator)).append(lineBreak));
        return lines.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static @Nullable Byte lastByteBefore(FileChannel source, long at) throws IOException {
        if (at == 0) {
            return null;
        }
        var one = ByteBuffer.allocate(1);
        source.read(one, at - 1);
        return one.get(0);
    }

    /** Bytes of the file, from start to end, replaced with a text. */
    private record Change(long start, long end, String text) {

        /** The text as it is written into the file. */
        byte[] bytes() {
            return text.getBytes(StandardCharsets.UTF_8);
        }
    }
}
