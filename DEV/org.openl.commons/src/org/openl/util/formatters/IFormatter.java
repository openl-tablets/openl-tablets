package org.openl.util.formatters;

/**
 * @author snshor
 */
public interface IFormatter {
    String format(Object obj);

    /**
     * The value the whole text stands for, or {@code null} where it stands for none.
     *
     * <p>Text a value cannot be read out of is never answered with one: putting a value in place of what the
     * author wrote would say something they did not. What to do instead — draw the text as it stands, refuse
     * the write, take the element out of an array — is the caller's to decide.
     */
    Object parse(String value);
}
