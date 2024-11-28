package org.openl.rules.cloner;

/**
 * A wrapper class is used for cloning of the unmodifiable instances via the reference on the modifiable target.
 *
 * @author Yury Molchan
 */
final class Wrapper {
    Object unmodifiable;
    Object target;
}
