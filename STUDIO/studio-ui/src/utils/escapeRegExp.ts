/** Escapes a text so that it matches itself inside a regular expression. */
export const escapeRegExp = (text: string): string => text.replace(/[.*+?^${}()|[\]\\/]/g, String.raw`\$&`)
