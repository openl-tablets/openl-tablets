/** A localization file of a project, as read from its `i18n` folder. */
export interface MessageFile {
    /** The file name, `message_de_LU.properties`. */
    name: string
    /** What the file holds. */
    text: string
    /** Set when the file is not written in UTF-8: it is read as Windows-1252, and saved in UTF-8. */
    legacy: boolean
}
