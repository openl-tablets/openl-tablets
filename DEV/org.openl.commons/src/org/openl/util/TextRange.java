package org.openl.util;

/**
 * A piece of a text file, from a line and a column up to another, the end left out.
 *
 * <p>Lines count from 1. Columns count the characters of a line from 0, as a reader of the file reads them, so a
 * piece is found again in the text whatever line breaks it is written with.
 *
 * @param line      the line the piece begins on
 * @param column    the column of its first character
 * @param endLine   the line the piece ends on
 * @param endColumn the column after its last character
 */
public record TextRange(int line, int column, int endLine, int endColumn) {
}
