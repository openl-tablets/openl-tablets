import type { TextSpot } from '../services/projectStatus'

/** A language of the messages: a localization file of the project. */
export interface MessageLanguageView {
    /** The language, country and variant of the file name joined by `_`; empty for the default file. */
    tag: string
    /** Set when the file is not written in UTF-8: its first write writes it whole in UTF-8. */
    legacy?: boolean
}

/** What the check of the files warns about the value of a key in a language. */
export interface MessageRowWarning {
    language: string
    summary: string
    /** The pieces of the value the warning is about; absent for the value as a whole. */
    spots?: TextSpot[]
}

/** The value a project the project depends on gives a key in a language. */
export interface SharedMessageText {
    value: string
    /** The name of the project the value comes from. */
    project: string
    /** The id of that project, when the workspace holds it. */
    projectId?: string
}

/** A key of the messages with its values in every language of the project. */
export interface MessageRow {
    key: string
    /** The values by the language that writes the key; null for a key written without a value. */
    values: Record<string, string | null>
    /** The values the projects the project depends on give the key, by the language. */
    shared?: Record<string, SharedMessageText>
    warnings?: MessageRowWarning[]
}

/** A page of the keys of the messages, with the languages of the project. */
export interface MessagesPage {
    /** Absent for a project without localization files. */
    languages?: MessageLanguageView[]
    content?: MessageRow[]
    pageNumber: number
    pageSize: number
    numberOfElements: number
    total?: number
}

/** One change of the localization files, as the server takes it. */
export type LocalizationAction =
    | { operation: 'setValue', key: string, language: string, value: string }
    | { operation: 'removeValue', key: string, language: string }
    | { operation: 'addKey', key: string, values: Record<string, string> }
    | { operation: 'removeKey', key: string }
    | { operation: 'addLanguage', language: string, values?: Record<string, string> }
