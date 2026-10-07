/**
 * Whether a new project name holds a slash or a backslash.
 *
 * The name travels in the address of the request that creates the project. The server refuses such an address
 * before it checks the name, and gives no reason. The dialogs refuse these two characters themselves, with the
 * message the server gives for every other character a project name cannot hold.
 */
export const breaksProjectAddress = (name: string): boolean => /[/\\]/.test(name)
