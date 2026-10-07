package org.openl.rules.ui;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Lets one write at a time into the workbooks a session holds.
 *
 * <p>A workbook is an Apache POI object over an XmlBeans store, and neither is safe to touch from two threads at
 * once. Two requests writing cells into the same workbook and saving it tear that store apart: a cursor list left
 * with a broken link spins at a whole core until the server is restarted, a cell lands at index -1, and the save
 * fails the store's own assertion halfway through.
 *
 * <p>One lock for the whole session rather than one per workbook, although a session writes several. The
 * workbooks live in the module the session has compiled, and the session outlives the resets that swap it; and
 * one lock cannot be taken out of order, while a write that touches two workbooks — a table copied from one
 * into another — would have to take two. A session writing two of its workbooks at once is not worth a lock
 * order.
 *
 * <p>Taken by a request thread and by nothing else, before anything the request goes on to lock, so the
 * compilation worker — which holds the module and the dependency manager — cannot meet it in a cycle.
 *
 * @author Vladyslav Pikus
 */
public class WorkbookWrites {

    private final ReentrantLock oneAtATime = new ReentrantLock();

    /**
     * Runs a write of the session's workbooks, waiting for the write before it to have written its file.
     *
     * @param write the write to run
     * @return whatever the write answers
     */
    public <T> T writing(Supplier<T> write) {
        oneAtATime.lock();
        try {
            return write.get();
        } finally {
            oneAtATime.unlock();
        }
    }
}
