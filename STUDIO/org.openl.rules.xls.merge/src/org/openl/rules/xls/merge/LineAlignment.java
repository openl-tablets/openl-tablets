package org.openl.rules.xls.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * How the lines of a table of BASE revision, its rows or its columns, are found in another revision of the table.
 *
 * <p>Lines are compared by their content, the same way a text diff compares lines of text. A run of changed lines
 * that has as many lines in both revisions is taken as those lines changed in place. In a run with more lines in one
 * revision, the lines that keep most of their cells are taken as changed in place, and the others as inserted or
 * deleted. What is left is a structural change: lines inserted, deleted, or replaced by a different number of lines.
 */
@NullMarked
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class LineAlignment {

    /**
     * The most comparisons of lines an alignment may take.
     */
    private static final long MAX_COMPARISONS = 2_000_000L;

    /**
     * The most pairs of lines a run of changed lines may compare cell by cell.
     */
    private static final long MAX_SIMILARITIES = 10_000L;

    /**
     * The line of the other revision by the line of BASE revision, or {@code -1} for a line the other revision does
     * not keep.
     */
    private final int[] sideOf;
    /**
     * The line of BASE revision by the line of the other revision, or {@code -1} for a line the other revision
     * inserted.
     */
    private final int[] baseOf;
    /**
     * The structural changes, from the first line to the last.
     */
    private final List<Hunk> hunks;

    /**
     * How alike a line of BASE revision and a line of the other revision are.
     */
    @FunctionalInterface
    interface Similarity {

        /**
         * @return the number of cells the lines share, or a negative number when the lines are too different to be
         * one line changed in place
         */
        int of(int baseLine, int sideLine);
    }

    /**
     * Lines {@code baseFrom..baseTo-1} of BASE revision replaced by lines {@code sideFrom..sideTo-1} of the other
     * revision, a different number of lines. An empty range of BASE revision is an insertion before line
     * {@code baseFrom}, an empty range of the other revision is a deletion.
     */
    record Hunk(int baseFrom, int baseTo, int sideFrom, int sideTo) {
    }

    /**
     * A line of BASE revision and the line of the other revision taken as the same line.
     */
    private record Pair(int baseLine, int sideLine) {
    }

    /**
     * Tells whether the other revision inserted or deleted lines.
     */
    boolean isStructural() {
        return !hunks.isEmpty();
    }

    /**
     * Aligns the lines of BASE revision with the lines of another revision by their digests.
     *
     * @param similarity tells how alike two lines are, to find the lines changed in place among inserted ones
     * @return the alignment, or {@code null} when too many lines changed to align them
     */
    static @Nullable LineAlignment align(long[] base, long[] side, Similarity similarity) {
        var prefix = 0;
        while (prefix < base.length && prefix < side.length && base[prefix] == side[prefix]) {
            prefix++;
        }
        var suffix = 0;
        while (suffix < base.length - prefix && suffix < side.length - prefix
                && base[base.length - 1 - suffix] == side[side.length - 1 - suffix]) {
            suffix++;
        }
        var n = base.length - prefix - suffix;
        var m = side.length - prefix - suffix;
        var sideOf = new int[base.length];
        var baseOf = new int[side.length];
        Arrays.fill(sideOf, -1);
        Arrays.fill(baseOf, -1);
        for (var i = 0; i < prefix; i++) {
            pair(sideOf, baseOf, i, i);
        }
        for (var i = 0; i < suffix; i++) {
            pair(sideOf, baseOf, base.length - 1 - i, side.length - 1 - i);
        }
        if (!matchMiddle(base, side, new Hunk(prefix, prefix + n, prefix, prefix + m), sideOf, baseOf)) {
            return null;
        }
        return new LineAlignment(sideOf, baseOf, pairChangedRuns(sideOf, baseOf, similarity));
    }

    /**
     * Pairs the equal lines of parts of two sequences by their longest common subsequence. Parts too long to compare
     * every line with every other one are first cut at the lines that occur once in each part, the way a patience diff
     * does.
     *
     * @param part the lines {@code baseFrom..baseTo-1} and {@code sideFrom..sideTo-1} to pair
     * @return {@code false} when a part is too long to compare and has no such lines to cut it at
     */
    private static boolean matchMiddle(long[] base, long[] side, Hunk part, int[] sideOf, int[] baseOf) {
        var n = part.baseTo() - part.baseFrom();
        var m = part.sideTo() - part.sideFrom();
        if (n == 0 || m == 0) {
            return true;
        } else if ((long) n * m <= MAX_COMPARISONS) {
            matchAll(base, side, part, sideOf, baseOf);
            return true;
        }
        var anchors = uniqueAnchors(base, side, part);
        if (anchors.isEmpty()) {
            return false;
        }
        var baseFrom = part.baseFrom();
        var sideFrom = part.sideFrom();
        for (Pair anchor : anchors) {
            if (!matchMiddle(base, side, new Hunk(baseFrom, anchor.baseLine(), sideFrom, anchor.sideLine()), sideOf,
                    baseOf)) {
                return false;
            }
            pair(sideOf, baseOf, anchor.baseLine(), anchor.sideLine());
            baseFrom = anchor.baseLine() + 1;
            sideFrom = anchor.sideLine() + 1;
        }
        return matchMiddle(base, side, new Hunk(baseFrom, part.baseTo(), sideFrom, part.sideTo()), sideOf, baseOf);
    }

    private static void matchAll(long[] base, long[] side, Hunk part, int[] sideOf, int[] baseOf) {
        var n = part.baseTo() - part.baseFrom();
        var m = part.sideTo() - part.sideFrom();
        // lengths[i][j] is the longest common subsequence of the lines from i and from j to the end of the part
        var lengths = new int[n + 1][m + 1];
        for (var i = n - 1; i >= 0; i--) {
            for (var j = m - 1; j >= 0; j--) {
                lengths[i][j] = base[part.baseFrom() + i] == side[part.sideFrom() + j]
                        ? lengths[i + 1][j + 1] + 1
                        : Math.max(lengths[i + 1][j], lengths[i][j + 1]);
            }
        }
        var i = 0;
        var j = 0;
        while (i < n && j < m) {
            if (base[part.baseFrom() + i] == side[part.sideFrom() + j]) {
                pair(sideOf, baseOf, part.baseFrom() + i, part.sideFrom() + j);
                i++;
                j++;
            } else if (lengths[i + 1][j] >= lengths[i][j + 1]) {
                i++;
            } else {
                j++;
            }
        }
    }

    /**
     * Finds the lines that occur once in each part with the same content, and keeps the longest run of them that
     * stands in the same order in both parts.
     *
     * @return the lines, in order
     */
    private static List<Pair> uniqueAnchors(long[] base, long[] side, Hunk part) {
        var baseLines = uniqueLines(base, part.baseFrom(), part.baseTo());
        var candidates = new ArrayList<Pair>();
        var sideLines = uniqueLines(side, part.sideFrom(), part.sideTo());
        for (var j = part.sideFrom(); j < part.sideTo(); j++) {
            var baseLine = baseLines.get(side[j]);
            if (baseLine != null && baseLine >= 0 && sideLines.get(side[j]) == j) {
                candidates.add(new Pair(baseLine, j));
            }
        }
        return longestGrowingRun(candidates);
    }

    /**
     * Keeps the longest run of pairs whose lines of BASE revision grow, the way patience sorting finds it.
     *
     * @param candidates the pairs, by their lines of the other revision
     * @return the run, in order
     */
    private static List<Pair> longestGrowingRun(List<Pair> candidates) {
        var tails = new ArrayList<Integer>();
        var previous = new int[candidates.size()];
        for (var k = 0; k < candidates.size(); k++) {
            var low = firstTailNotBelow(candidates, tails, candidates.get(k).baseLine());
            previous[k] = low > 0 ? tails.get(low - 1) : -1;
            if (low == tails.size()) {
                tails.add(k);
            } else {
                tails.set(low, k);
            }
        }
        var anchors = new ArrayList<Pair>();
        for (var k = tails.isEmpty() ? -1 : tails.getLast(); k >= 0; k = previous[k]) {
            anchors.add(candidates.get(k));
        }
        return anchors.reversed();
    }

    /**
     * Finds the shortest run whose last line of BASE revision is not below a line.
     *
     * @param tails the last pair of each run, by the length of the run
     */
    private static int firstTailNotBelow(List<Pair> candidates, List<Integer> tails, int line) {
        var low = 0;
        var high = tails.size();
        while (low < high) {
            var middle = (low + high) >>> 1;
            if (candidates.get(tails.get(middle)).baseLine() < line) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    /**
     * The line of each content that occurs once in a part of a sequence, and {@code -1} for a content that occurs more
     * than once.
     */
    private static Map<Long, Integer> uniqueLines(long[] lines, int from, int to) {
        var unique = new HashMap<Long, Integer>();
        for (var i = from; i < to; i++) {
            var line = i;
            unique.merge(lines[i], line, (first, again) -> -1);
        }
        return unique;
    }

    /**
     * Pairs the changed lines of every run of changed lines, and lists the rest as structural changes.
     */
    private static List<Hunk> pairChangedRuns(int[] sideOf, int[] baseOf, Similarity similarity) {
        var hunks = new ArrayList<Hunk>();
        var baseFrom = 0;
        var sideFrom = 0;
        while (baseFrom <= sideOf.length && sideFrom <= baseOf.length) {
            var baseTo = baseFrom;
            while (baseTo < sideOf.length && sideOf[baseTo] < 0) {
                baseTo++;
            }
            var sideTo = sideFrom;
            while (sideTo < baseOf.length && baseOf[sideTo] < 0) {
                sideTo++;
            }
            pairRun(new Hunk(baseFrom, baseTo, sideFrom, sideTo), sideOf, baseOf, similarity, hunks);
            // the lines at baseTo and sideTo are a pair, or the ends of both sequences
            baseFrom = baseTo + 1;
            sideFrom = sideTo + 1;
        }
        return List.copyOf(hunks);
    }

    /**
     * Pairs the lines of a run of changed lines: all of them when the run has as many lines in both revisions, and
     * otherwise the most alike lines, in order. The lines left between the pairs are paired the same way when there
     * are as many of them in both revisions, and are a structural change otherwise.
     */
    private static void pairRun(Hunk run, int[] sideOf, int[] baseOf, Similarity similarity, List<Hunk> hunks) {
        var k = run.baseTo() - run.baseFrom();
        var m = run.sideTo() - run.sideFrom();
        if (k == m) {
            for (var i = 0; i < k; i++) {
                pair(sideOf, baseOf, run.baseFrom() + i, run.sideFrom() + i);
            }
            return;
        }
        var baseLine = run.baseFrom();
        var sideLine = run.sideFrom();
        if (k > 0 && m > 0 && (long) k * m <= MAX_SIMILARITIES) {
            for (var pair : alike(run, similarity)) {
                pairRun(new Hunk(baseLine, pair.baseLine(), sideLine, pair.sideLine()), sideOf, baseOf, similarity,
                        hunks);
                pair(sideOf, baseOf, pair.baseLine(), pair.sideLine());
                baseLine = pair.baseLine() + 1;
                sideLine = pair.sideLine() + 1;
            }
        }
        if (baseLine == run.baseFrom() && sideLine == run.sideFrom()) {
            hunks.add(run);
        } else {
            pairRun(new Hunk(baseLine, run.baseTo(), sideLine, run.sideTo()), sideOf, baseOf, similarity, hunks);
        }
    }

    /**
     * Finds the pairs of alike lines of a run, in order, that share the most cells.
     *
     * @return the pairs, in order
     */
    private static List<Pair> alike(Hunk run, Similarity similarity) {
        var k = run.baseTo() - run.baseFrom();
        var m = run.sideTo() - run.sideFrom();
        var shared = shared(run, similarity);
        // best[i][j] is the most cells the pairs of the lines from i and from j to the end of the run share
        var best = new int[k + 1][m + 1];
        for (var i = k - 1; i >= 0; i--) {
            for (var j = m - 1; j >= 0; j--) {
                var paired = shared[i][j] < 0 ? -1 : best[i + 1][j + 1] + shared[i][j];
                best[i][j] = Math.max(paired, Math.max(best[i + 1][j], best[i][j + 1]));
            }
        }
        var pairs = new ArrayList<Pair>();
        var i = 0;
        var j = 0;
        while (i < k && j < m) {
            if (shared[i][j] >= 0 && best[i][j] == best[i + 1][j + 1] + shared[i][j]) {
                pairs.add(new Pair(run.baseFrom() + i, run.sideFrom() + j));
                i++;
                j++;
            } else if (best[i + 1][j] >= best[i][j + 1]) {
                i++;
            } else {
                j++;
            }
        }
        return pairs;
    }

    /**
     * How many cells every line of BASE revision of a run shares with every line of the other revision.
     */
    private static int[][] shared(Hunk run, Similarity similarity) {
        var shared = new int[run.baseTo() - run.baseFrom()][run.sideTo() - run.sideFrom()];
        for (var i = 0; i < shared.length; i++) {
            for (var j = 0; j < shared[i].length; j++) {
                shared[i][j] = similarity.of(run.baseFrom() + i, run.sideFrom() + j);
            }
        }
        return shared;
    }

    private static void pair(int[] sideOf, int[] baseOf, int baseLine, int sideLine) {
        sideOf[baseLine] = sideLine;
        baseOf[sideLine] = baseLine;
    }
}
