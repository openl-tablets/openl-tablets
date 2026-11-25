/**
 * The text that follows each place where a line may break in an element, as `withPathBreaks` draws it.
 *
 * A place to break before a dot of a path is followed by that dot and the part after it, such as `.coverageDetail`.
 */
export const afterBreaks = (element: Element): (string | null | undefined)[] =>
    [...element.querySelectorAll('wbr')].map(place => place.nextSibling?.textContent)
