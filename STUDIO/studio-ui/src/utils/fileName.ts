/** The characters the server's `NameChecker` refuses in the name of a file or a folder. */
const FORBIDDEN = /[\\/:;<>?*%'[\]|"]/

/**
 * Whether a name can be given to a file or a folder of a project.
 *
 * The server refuses a name that holds one of the characters `\ / : ; < > ? * % ' [ ] | "`, starts with a space,
 * or ends with a space or a dot. A dialog checks the same rule as the name is typed, so the reason shows under
 * the field instead of in a notification after the request. The server still has the last word.
 */
export const isValidFileName = (name: string): boolean => !FORBIDDEN.test(name) && !name.startsWith(' ')
    && !/[ .]$/.test(name)
