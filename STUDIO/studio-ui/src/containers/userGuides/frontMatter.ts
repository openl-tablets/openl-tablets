/**
 * The text of a page as a reader sees it.
 *
 * Kept apart from everything that knows the address of the guides, so the search worker can read pages too.
 */

/** A page without its front matter, which the documentation site reads and a reader is not shown. */
export const withoutFrontMatter = (text: string): string =>
    text.replace(/^---\r?\n[\s\S]*?\r?\n---[ \t]*(?:\r?\n|$)/, '')

/** Whether a page opens with a heading of its own, which then stands for its title. */
export const startsWithHeading = (text: string): boolean => /^\s*#{1,6}\s/.test(withoutFrontMatter(text))
