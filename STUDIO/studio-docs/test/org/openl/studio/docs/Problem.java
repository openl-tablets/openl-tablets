package org.openl.studio.docs;

/**
 * What breaks a file of the user guides.
 *
 * @param file the file, relative to the guides folder
 * @param line the line of the file, counted from 1, or 0 for the file as a whole
 * @author Yury Molchan
 */
record Problem(String file, int line, String message) {

    @Override
    public String toString() {
        return file + (line > 0 ? ":" + line : "") + ": " + message;
    }
}
