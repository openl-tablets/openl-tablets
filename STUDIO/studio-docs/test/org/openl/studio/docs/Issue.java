package org.openl.studio.docs;

/**
 * What is wrong inside a code block of a user guide.
 *
 * @param line the line of the block, counted from 0
 * @author Yury Molchan
 */
record Issue(int line, String message) {
}
