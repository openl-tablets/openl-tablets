import apiCall, { LOCAL_LOAD_API_OPTIONS } from './apiCall'
import type { ProjectStatusDetailedMessage } from './projectStatus'

/** The folder of a project the engine reads the messages from. */
export const BUNDLE_FOLDER = 'i18n'

/**
 * The name of a localization file: `message.properties`, or `message_` with a language tag, as
 * `message_de_LU.properties`: a language of two or three letters, then the country and the variant.
 */
const MESSAGE_FILE = /^message(_[A-Za-z]{2,3}(?:_\w*[A-Za-z0-9])?)?\.properties$/

/** Whether a path of a project names one of its localization files, the ones the Localization screen edits. */
export const isMessageFilePath = (path: string): boolean => {
    const slash = path.lastIndexOf('/')
    return path.slice(0, slash) === BUNDLE_FOLDER && MESSAGE_FILE.test(path.slice(slash + 1))
}

/**
 * What the places of a problem of a localization file are counted from: the pieces of the text of the key, which a
 * screen of the texts marks, or the places of the file, which an editor of the file marks.
 */
export type MessagePositions = 'text' | 'file'

/**
 * Checks the localization files of a project, without compiling it: what is wrong with each text, by its key and
 * language, each problem placed as the positions say. A project nobody opened is not read, and nothing is found about
 * it.
 */
export async function checkMessageFiles(
    projectId: string,
    positions: MessagePositions
): Promise<ProjectStatusDetailedMessage[]> {
    return await apiCall(
        `/projects/${projectId}/localization/problems?positions=${positions}`,
        undefined,
        LOCAL_LOAD_API_OPTIONS
    ) as ProjectStatusDetailedMessage[]
}
