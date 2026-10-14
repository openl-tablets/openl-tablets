package org.openl.rules.repository.git.text;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;

/**
 * Merges a text file two branches changed since their base line by line, and tells what of it the merge took by
 * itself and where the branches conflict.
 *
 * <p>The lines a single branch changed are taken from it, as Git takes them. Changes of both branches that overlap
 * or touch one another are a conflict, unless the branches made them alike.
 *
 * <p>A conflict holds every line each branch changed in its place, the lines both versions share included, and the
 * base lines they replaced. Git's conflict markers leave out the lines both versions start or end with, while
 * taking both versions here keeps each of them whole.
 *
 * <p>The file is told part by part, each part with the lines every version holds in its place, so the versions can
 * be laid side by side. The lines come with their line breaks: the parts put together in their order, one version
 * taken of each conflict, give the file back with the line breaks it was written with.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TextMerge {

    /** The algorithm Git compares the lines of a text file with when nothing configures another one. */
    private static final DiffAlgorithm DIFF = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM);

    /** A part of a merged text file. */
    public sealed interface Block permits Merged, Conflict {
    }

    /**
     * Lines the merge took by itself, with the lines each version holds in their place: lines both branches left
     * alone, lines a single branch changed, or lines both branches changed alike.
     *
     * @param text   the lines the merge took, with their line breaks
     * @param ours   the lines of the branch merged into
     * @param base   the lines both branches started from
     * @param theirs the lines of the branch merged in
     */
    public record Merged(String text, String ours, String base, String theirs) implements Block {

        /** Lines no branch changed, which every version holds as they are. */
        public Merged(String text) {
            this(text, text, text, text);
        }

        /** Tells whether a branch changed the lines. */
        public boolean changed() {
            return !ours.equals(base) || !theirs.equals(base);
        }
    }

    /**
     * Lines the two branches changed in different ways, each version of them with its line breaks.
     *
     * @param ours   the lines of the branch merged into
     * @param base   the lines both branches started from; empty when they added the lines
     * @param theirs the lines of the branch merged in
     */
    public record Conflict(String ours, String base, String theirs) implements Block {
    }

    /**
     * Merges three versions of a text file.
     *
     * @param base   the version both branches started from; empty when they added the file
     * @param ours   the version of the branch merged into
     * @param theirs the version of the branch merged in
     * @return the blocks of the merged file in their order, or empty when a version is no UTF-8 text
     */
    public static Optional<List<Block>> merge(byte[] base, byte[] ours, byte[] theirs) {
        if (Stream.of(base, ours, theirs).anyMatch(version -> !isText(version))) {
            return Optional.empty();
        }
        var baseText = new RawText(base);
        var ourSide = new Side(baseText, new RawText(ours));
        var theirSide = new Side(baseText, new RawText(theirs));
        var blocks = new ArrayList<Block>();
        int line = 0;
        while (ourSide.hasChanges() || theirSide.hasChanges()) {
            int begin = Math.min(ourSide.nextChange(), theirSide.nextChange());
            addUnchanged(blocks, baseText.getString(line, begin, false));
            ourSide.start(begin);
            theirSide.start(begin);
            // The changes of both branches that overlap or touch one another replace one range of base lines.
            int end = begin;
            int reached;
            do {
                reached = end;
                end = theirSide.take(ourSide.take(end));
            } while (end != reached);
            var ourLines = ourSide.lines(end);
            var baseLines = baseText.getString(begin, end, false);
            var theirLines = theirSide.lines(end);
            if (theirLines.equals(baseLines) || ourLines.equals(theirLines)) {
                blocks.add(new Merged(ourLines, ourLines, baseLines, theirLines));
            } else if (ourLines.equals(baseLines)) {
                blocks.add(new Merged(theirLines, ourLines, baseLines, theirLines));
            } else {
                blocks.add(new Conflict(ourLines, baseLines, theirLines));
            }
            line = end;
        }
        addUnchanged(blocks, baseText.getString(line, baseText.size(), false));
        return Optional.of(List.copyOf(blocks));
    }

    /** Adds lines no branch changed, unless there are none. */
    private static void addUnchanged(List<Block> blocks, String text) {
        if (!text.isEmpty()) {
            blocks.add(new Merged(text));
        }
    }

    /**
     * Tells whether content is text that a merge reads line by line: UTF-8 with no NUL character in its first bytes,
     * the sign by which Git tells binary content, as Git reads only those. Content that starts as binary is told so
     * without the rest being read; the rest is decoded a part at a time.
     *
     * @param content the content, read up to its end unless it starts as binary content
     */
    public static boolean isText(InputStream content) throws IOException {
        var head = content.readNBytes(RawText.getBufferSize());
        for (var octet : head) {
            if (octet == 0) {
                return false;
            }
        }
        // A reader given the decoder itself reports a malformed sequence instead of replacing it.
        try (var reader = new InputStreamReader(new SequenceInputStream(new ByteArrayInputStream(head), content),
                StandardCharsets.UTF_8.newDecoder())) {
            reader.transferTo(Writer.nullWriter());
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    private static boolean isText(byte[] content) {
        try {
            return isText(new ByteArrayInputStream(content));
        } catch (IOException e) {
            // Content in memory is read without a failure.
            throw new UncheckedIOException(e);
        }
    }

    /** A branch: its version of the file and the changes it made to the base, taken range by range of base lines. */
    private static final class Side {

        private final RawText text;
        private final Deque<Edit> changes;
        /** How many lines the branch has more than the base before the base line the merge has reached. */
        private int shift;
        /** The line of the branch the current range starts at. */
        private int begin;

        Side(RawText base, RawText text) {
            this.text = text;
            changes = new ArrayDeque<>(DIFF.diff(RawTextComparator.DEFAULT, base, text));
        }

        boolean hasChanges() {
            return !changes.isEmpty();
        }

        /** The base line the next change starts at; no line when the branch made no more changes. */
        int nextChange() {
            var change = changes.peek();
            return change == null ? Integer.MAX_VALUE : change.getBeginA();
        }

        /** Starts a range at a base line. */
        void start(int line) {
            begin = line + shift;
        }

        /**
         * Takes into the range the changes that start at its end or before it.
         *
         * @param end the base line the range ends at
         * @return the base line the range ends at with the changes taken
         */
        int take(int end) {
            int reached = end;
            while (nextChange() <= reached) {
                var change = changes.remove();
                reached = Math.max(reached, change.getEndA());
                shift += change.getLengthB() - change.getLengthA();
            }
            return reached;
        }

        /** The lines of the branch in the range, which ends at the base line given. */
        String lines(int end) {
            return text.getString(begin, end + shift, false);
        }
    }
}
