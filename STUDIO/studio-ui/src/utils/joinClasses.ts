/**
 * The class names given, joined by spaces; a name left out — false, null or undefined — is skipped.
 *
 * It only joins the names. A screen marking the cells of a table hands them to the grid this way, and the grid puts
 * them together with its own classes once for every set of them it draws.
 */
export const joinClasses = (...names: Array<string | false | null | undefined>): string =>
    names.filter(Boolean).join(' ')
