/**
 * What the screens offer for reading the results of a test run.
 *
 * <p>These are the reader's choices, not the wire: a count of {@link ALL_FAILURES} and a size of
 * {@link ALL_TESTS_ON_A_PAGE} are turned into their own flags when the request is built.
 */

/** How many test tables one page of the results holds, and the sizes the screen offers instead. */
export const TESTS_PAGE_SIZE = 20
/** A size that puts every test table on one page. */
export const ALL_TESTS_ON_A_PAGE = -1
export const TESTS_PAGE_SIZES = [1, 5, 20, ALL_TESTS_ON_A_PAGE]

/** How many failures of a test table the results show, and the counts the screen offers instead. */
export const FAILURES_PER_TEST = 5
/** A count that lists every failure of a test table. */
export const ALL_FAILURES = -1
export const FAILURES_PER_TEST_OPTIONS = [1, 5, 20, ALL_FAILURES]
