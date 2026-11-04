package org.openl.rules.repository.git;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Set;
import java.util.TreeSet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.BinaryBlobException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeAlgorithm;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.pack.PackConfig;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.jspecify.annotations.Nullable;

import org.openl.rules.repository.IgnoredPaths;
import org.openl.rules.xls.merge.XlsWorkbookMerger;
import org.openl.util.FileTypeHelper;

/**
 * Tells which files may conflict when two commits are merged. Nothing is merged and nothing is written.
 *
 * <p>The trees of the two commits are compared with the commit they parted at first, so only the files both commits
 * changed, each in its own way, are read at all. Of them, a text file may conflict when its lines changed by both
 * overlap. A workbook may conflict when both commits changed the same cells, rows or columns of a table, the way the
 * merge compares the versions before it combines them. Any other file changed by both may conflict. So may a file
 * one commit deleted and the other one changed, and a workbook both commits added.
 *
 * <p>It is an estimate: the merge also stops at a workbook it cannot write, such as one whose formula taken from
 * the other version reads a workbook the version merged into numbers differently. A file both changed that cannot be
 * read may conflict, so one unreadable file does not take the estimate of the others with it. The estimate stops when
 * the thread asking for it is interrupted.
 *
 * <p>An ignored file never conflicts: the merge keeps the version of the branch it goes into. Of several commits the
 * histories parted at, as a criss-cross history has, the first one stands for the base.
 */
@Slf4j
@RequiredArgsConstructor
final class TrialMerge {

    private final Repository repository;
    private final IgnoredPaths ignoredPaths;
    private final BlobReader workbookReader;

    /** Reads a workbook kept in the repository: a file kept in LFS is read as it is, not as its pointer. */
    @FunctionalInterface
    interface BlobReader {
        ObjectLoader read(ObjectLoader loader) throws IOException;
    }

    /**
     * The files under a folder that may conflict when the two commits are merged.
     *
     * @param base   the commit both parted at, or {@code null} when their histories have none in common
     * @param ours   a commit
     * @param theirs another commit
     * @param path   the folder, from the root of the repository; an empty path is the whole repository
     * @return the paths of the conflicting files from the root of the repository, in their order
     * @throws InterruptedIOException when the thread is interrupted
     */
    Set<String> conflictsOf(@Nullable AnyObjectId base, AnyObjectId ours, AnyObjectId theirs, String path)
            throws IOException {
        var conflicts = new TreeSet<String>();
        try (var treeWalk = new TreeWalk(repository); var walk = new RevWalk(repository)) {
            if (base == null) {
                treeWalk.addTree(new EmptyTreeIterator());
            } else {
                treeWalk.addTree(walk.parseCommit(base).getTree());
            }
            treeWalk.addTree(walk.parseCommit(ours).getTree());
            treeWalk.addTree(walk.parseCommit(theirs).getTree());
            treeWalk.setRecursive(true);
            treeWalk.setFilter(path.isEmpty()
                    ? TreeFilter.ANY_DIFF
                    : AndTreeFilter.create(PathFilter.create(path), TreeFilter.ANY_DIFF));
            while (treeWalk.next()) {
                var file = treeWalk.getPathString();
                if (changedByBoth(treeWalk) && mayConflict(file, treeWalk)) {
                    conflicts.add(file);
                }
            }
        }
        return conflicts;
    }

    /** Whether the walk of the base, ours and theirs stands on a file where all three differ. */
    private static boolean changedByBoth(TreeWalk treeWalk) {
        return !GitRepository.sameEntry(treeWalk, 1, 2)
                && !GitRepository.sameEntry(treeWalk, 0, 1)
                && !GitRepository.sameEntry(treeWalk, 0, 2);
    }

    /**
     * Whether the file the walk stands on may conflict. The walk holds the base, then the two commits, and stands on a
     * file both commits changed. An ignored file never conflicts, and one that cannot be read may.
     *
     * @throws InterruptedIOException when the thread is interrupted, before the file is read or while it is
     */
    boolean mayConflict(String file, TreeWalk treeWalk) throws InterruptedIOException {
        // A workbook takes seconds to read, so the estimate can be stopped before each file.
        stopIfInterrupted();
        if (ignoredPaths.matches(file)) {
            return false;
        }
        try {
            return conflicts(file, treeWalk);
        } catch (IOException | RuntimeException e) {
            // A read cut off by the interrupt stops the estimate, it does not tell of the file.
            stopIfInterrupted();
            log.debug("The file '{}' cannot be read to estimate whether it conflicts.", file, e);
            return true;
        }
    }

    private static void stopIfInterrupted() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("The estimate of the conflicts is cancelled.");
        }
    }

    private boolean conflicts(String file, TreeWalk treeWalk) throws IOException {
        var base = fileOf(treeWalk, 0);
        var ours = fileOf(treeWalk, 1);
        var theirs = fileOf(treeWalk, 2);
        if (ours == null || theirs == null) {
            // Deleted by one and changed by the other, or a file in one and a folder in the other
            return true;
        }
        if (FileTypeHelper.isExcelFile(file)) {
            // The merge combines a workbook only against the version both parted from
            return base == null || changesOverlap(base, ours, theirs);
        }
        return textsConflict(base, ours, theirs);
    }

    /** The content of a file the tree holds, or {@code null} when it holds no file at the path. */
    private static @Nullable ObjectId fileOf(TreeWalk treeWalk, int tree) {
        var isFile = (treeWalk.getRawMode(tree) & FileMode.TYPE_MASK) == FileMode.TYPE_FILE;
        return isFile ? treeWalk.getObjectId(tree) : null;
    }

    /** Whether the lines of a text file both commits changed overlap. A binary file is never combined. */
    private boolean textsConflict(@Nullable ObjectId base, ObjectId ours, ObjectId theirs) throws IOException {
        try {
            var merged = new MergeAlgorithm().merge(RawTextComparator.DEFAULT,
                    textOf(base),
                    textOf(ours),
                    textOf(theirs));
            return merged.containsConflicts();
        } catch (BinaryBlobException e) {
            return true;
        }
    }

    private RawText textOf(@Nullable ObjectId blob) throws IOException, BinaryBlobException {
        return blob == null
                ? RawText.EMPTY_TEXT
                : RawText.load(repository.open(blob, Constants.OBJ_BLOB), PackConfig.DEFAULT_BIG_FILE_THRESHOLD);
    }

    /** Whether both commits changed the same cells, rows or columns of a workbook, as the merge compares its versions. */
    private boolean changesOverlap(ObjectId base, ObjectId ours, ObjectId theirs) throws IOException {
        try (var baseBook = workbookOf(base);
             var ourBook = workbookOf(ours);
             var theirBook = workbookOf(theirs);
             var merger = XlsWorkbookMerger.create(baseBook, ourBook, theirBook)) {
            return merger.getDiffResult().hasConflicts();
        }
    }

    private InputStream workbookOf(ObjectId blob) throws IOException {
        return workbookReader.read(repository.open(blob, Constants.OBJ_BLOB)).openStream();
    }
}
