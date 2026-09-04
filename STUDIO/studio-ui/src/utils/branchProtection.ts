/**
 * Whether a branch is protected, told from the repository's protected-branch glob patterns.
 *
 * The patterns come from the repository configuration and are the same the server matches a branch against,
 * so a branch reads as protected in the forms exactly when the server would refuse a direct commit to it.
 */

/** Escapes one character so it matches itself inside a regular expression. */
const escapeRegExpChar = (char: string): string => char.replace(/[.*+?^${}()|[\]\\/]/g, String.raw`\$&`)

/** Translates a `[...]` set, returning the regex chunk and how many characters it consumed. */
const translateClass = (glob: string, start: number): [string, number] => {
    let end = start + 1
    // A leading '!' negates the set; a ']' right after the opening (or the negation) is a literal member.
    if (glob[end] === '!') {
        end += 1
    }
    if (glob[end] === ']') {
        end += 1
    }
    while (end < glob.length && glob[end] !== ']') {
        end += 1
    }
    if (end >= glob.length) {
        // No closing bracket: the '[' stands for itself.
        return [escapeRegExpChar('['), 1]
    }
    const inner = glob.slice(start + 1, end)
    const negated = inner.startsWith('!')
    const members = negated ? inner.slice(1) : inner
    // Only '!' negates a glob set; a leading '^' is a literal member, so escape it rather than let the
    // regex read it as negation.
    const escaped = members.startsWith('^') ? `\\^${members.slice(1)}` : members
    const body = negated ? `^${escaped}` : escaped
    return [`[${body}]`, end - start + 1]
}

/**
 * Converts one branch glob pattern to a regular expression, matching the way the server does: `*` stops at a
 * path separator, `**` crosses it, `?` is a single non-separator character, `{a,b}` is a choice, and `[...]`
 * is a character set. The whole name must match.
 */
const globToRegExp = (glob: string): RegExp => {
    let source = ''
    let index = 0
    let inChoice = false
    while (index < glob.length) {
        const char = glob[index] ?? ''
        const nextChar = glob[index + 1]
        if (char === '\\') {
            source += nextChar ? escapeRegExpChar(nextChar) : '\\\\'
            index += nextChar ? 2 : 1
        } else if (char === '*' && nextChar === '*') {
            source += '.*'
            index += 2
        } else if (char === '*') {
            source += '[^/]*'
            index += 1
        } else if (char === '?') {
            source += '[^/]'
            index += 1
        } else if (char === '{') {
            source += '(?:'
            inChoice = true
            index += 1
        } else if (char === '}') {
            source += ')'
            inChoice = false
            index += 1
        } else if (char === ',' && inChoice) {
            source += '|'
            index += 1
        } else if (char === '[') {
            const [chunk, consumed] = translateClass(glob, index)
            source += chunk
            index += consumed
        } else {
            source += escapeRegExpChar(char)
            index += 1
        }
    }
    return new RegExp(`^${source}$`)
}

// A repository configures a handful of patterns that a whole list of branches is tested against, so each
// pattern is compiled once and reused. A pattern the browser cannot compile is remembered as null — it
// marks nothing, rather than breaking the form or being retried for every branch.
const compiled = new Map<string, RegExp | null>()

const compileGlob = (pattern: string): RegExp | null => {
    if (!compiled.has(pattern)) {
        try {
            compiled.set(pattern, globToRegExp(pattern))
        } catch {
            compiled.set(pattern, null)
        }
    }
    return compiled.get(pattern) ?? null
}

/** Whether the branch matches any of the protected-branch glob patterns. */
export const isProtectedBranch = (branch: string | undefined, patterns: string[] | undefined): boolean => {
    if (!branch || !patterns || patterns.length === 0) {
        return false
    }
    return patterns.some(pattern => compileGlob(pattern)?.test(branch) ?? false)
}
