export interface FsNode {
    path: string
    name: string
    basePath: string
    type: 'file' | 'folder'
    /**
     * A technical file of the project, such as `rules.xml` or `pom.xml`, hidden from the screens unless **Show Hidden
     * Files** is on. Present only when true.
     */
    hidden?: boolean
    /** File size in bytes (present for files). */
    size?: number
    /** File extension without the dot, e.g. "xlsx". */
    extension?: string
    /** Last modification timestamp (ISO). */
    lastModified?: string
}
