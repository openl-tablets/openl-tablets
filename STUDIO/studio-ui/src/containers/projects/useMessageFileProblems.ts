import { useEffect, useState } from 'react'
import { checkMessageFiles, isMessageFilePath } from '../../services/localization'
import type { ProjectStatusDetailedMessage } from '../../services/projectStatus'
import type { EditorProblem } from './CodeEditor'

/** The problems of one localization file among those the server found, each at every place of the file it is in. */
const problemsOf = (messages: readonly ProjectStatusDetailedMessage[], path: string): EditorProblem[] =>
    messages.flatMap(({ location, summary, severity }) => {
        if (location?.type !== 'localization' || location.path !== path) {
            return []
        }
        const told = { message: summary, severity: severity === 'ERROR' ? 'error' as const : 'warning' as const }
        // A message about the whole file is in no place of it.
        return location.places?.length ? location.places.map(place => ({ ...told, place })) : [told]
    })

/**
 * What the server finds wrong in a localization file, for the editor of the file to mark.
 *
 * <p>The server checks the files without compiling the project, once the text of the file is read, and again whenever
 * the text the file holds changes, as when it is saved: once for each. Any other file has no problems to mark: none are
 * given for it.
 *
 * @param projectId the project of the file
 * @param path      the path of the file in the project
 * @param text      the text the file holds, as it was read or saved, or null while it is read
 */
export const useMessageFileProblems = (projectId: string, path: string | null, text: string | null) => {
    const messageFile = path !== null && isMessageFilePath(path)
    const [problems, setProblems] = useState<{ of: string, problems: EditorProblem[] } | null>(null)
    useEffect(() => {
        if (!messageFile || path === null || text === null) {
            return undefined
        }
        let current = true
        void checkMessageFiles(projectId, 'file')
            .then(messages => problemsOf(messages, path))
            // A failed check leaves the text without marks: it can still be read and written.
            .catch(() => [])
            .then(found => {
                if (current) {
                    setProblems({ of: `${projectId}|${path}`, problems: found })
                }
            })
        return () => {
            current = false
        }
    }, [projectId, path, text, messageFile])
    if (!messageFile) {
        return undefined
    }
    // Those found for another file are not this one's. Those of the text before a save stay until the new ones come.
    return problems?.of === `${projectId}|${path}` ? problems.problems : undefined
}
