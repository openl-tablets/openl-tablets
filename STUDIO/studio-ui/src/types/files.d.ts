export interface FsNode {
    path: string
    name: string
    basePath: string
    type: 'file' | 'folder'
    /** File size in bytes (present for files). */
    size?: number
    /** File extension without the dot, e.g. "xlsx". */
    extension?: string
    /** Last modification timestamp (ISO). */
    lastModified?: string
}
